package org.garret.perst.impl;
import  org.garret.perst.*;
import  java.lang.ref.*;

public class WeakHashTable implements OidHashTable { 
    Entry table[];
    static final float loadFactor = 0.75f;
    int count;
    int threshold;
    int nModified;
    boolean disableRehash;
    StorageImpl db;

    public WeakHashTable(StorageImpl db, int initialCapacity) {
        this.db = db;
        threshold = (int)(initialCapacity * loadFactor);
        table = new Entry[initialCapacity];
    }

    public synchronized boolean remove(int oid) {
        Entry tab[] = table;
        int index = (oid & 0x7FFFFFFF) % tab.length;
        for (Entry e = tab[index], prev = null; e != null; prev = e, e = e.next) {
            if (e.oid == oid) {
                if (prev != null) {
                    prev.next = e.next;
                } else {
                    tab[index] = e.next;
                }
                e.clear();
                count -= 1;
                return true;
            }
        }
        return false;
    }

    protected Reference createReference(Object obj) { 
        return new WeakReference(obj);
    }

    public synchronized void put(int oid, Object obj) { 
        Reference ref = createReference(obj);
        Entry tab[] = table;
        int index = (oid & 0x7FFFFFFF) % tab.length;
        for (Entry e = tab[index]; e != null; e = e.next) {
            if (e.oid == oid) {
                e.ref = ref;
                return;
            }
        }
        if (count >= threshold && !disableRehash) {
            // Rehash the table if the threshold is exceeded
            rehash();
            tab = table;
            index = (oid & 0x7FFFFFFF) % tab.length;
        } 

        // Creates the new entry.
        tab[index] = new Entry(oid, ref, tab[index]);
        count += 1;
    }
    
    public Object get(int oid) {
        while (true) { 
            cs:synchronized(this) { 
                Entry tab[] = table;
                int index = (oid & 0x7FFFFFFF) % tab.length;
                for (Entry e = tab[index]; e != null; e = e.next) {
                    if (e.oid == oid) {
                        Object obj = e.ref.get();
                        if (obj == null) { 
                            if (e.dirty != 0) { 
                                break cs;
                            }
                        } else if (db.isDeleted(obj)) {
                            e.ref.clear();
                            return null;
                        }
                        return obj;
                    }
                }
                return null;
            }
            System.runFinalization();
        } 
    }
    
    public void flush() {
        while (true) { 
            cs:synchronized(this) { 
                disableRehash = true;
                int n;
                do { 
                    n = nModified;
                    for (int i = 0; i < table.length; i++) { 
                        for (Entry e = table[i]; e != null; e = e.next) { 
                            Object obj = e.ref.get();
                            if (obj != null) { 
                                if (db.isModified(obj)) { 
                                    db.store(obj);
                                }
                            } else if (e.dirty != 0) { 
                                break cs;
                            }
                        }
                    }
                } while (n != nModified);

                disableRehash = false;
                if (count >= threshold) {
                    // Rehash the table if the threshold is exceeded
                    rehash();
                }
                return;
            }
            System.runFinalization();
        }
    }

    public void invalidate() {
        while (true) { 
            cs:synchronized(this) { 
                for (int i = 0; i < table.length; i++) { 
                    for (Entry e = table[i]; e != null; e = e.next) { 
                        Object obj = e.ref.get();
                        if (obj != null) { 
                            if (db.isModified(obj)) { 
                                e.dirty = 0;
                                db.invalidate(obj);
                            }
                        } else if (e.dirty != 0) { 
                            break cs;
                        }
                    }
                }
                return;
            }
            System.runFinalization();
        }
    }

    public synchronized void reload() {
        disableRehash = true;
        for (int i = 0; i < table.length; i++) { 
            Entry e, next, prev;
            for (e = table[i]; e != null; e = e.next) {                 
                Object obj = e.ref.get();
                if (obj != null) { 
                    db.invalidate(obj);
                    try { 
                        db.load(obj);
                    } catch (Exception x) { 
                        // ignore errors caused by attempt to load object which was created in rollbacked transaction
                    }
                }
            }
        }
        disableRehash = false;
        if (count >= threshold) {
            // Rehash the table if the threshold is exceeded
            rehash();
        }
    }

    public synchronized void clear() {
        Entry tab[] = table;
        for (int i = 0; i < tab.length; i++) { 
            tab[i] = null;
        }
        count = 0;
    }

    void rehash() {
        int oldCapacity = table.length;
        Entry oldMap[] = table;
        int i;

        for (i = oldCapacity; --i >= 0;) {
            Entry e, next, prev;
            for (prev = null, e = oldMap[i]; e != null; e = next) { 
                next = e.next;
                Object obj = e.ref.get();
                if ((obj == null || db.isDeleted(obj)) && e.dirty == 0) { 
                    count -= 1;
                    e.clear();
                    if (prev == null) { 
                        oldMap[i] = next;
                    } else { 
                        prev.next = next;
                    }
                } else { 
                    prev = e;
                }
            }
        }
        if (count <= (threshold >>> 1)) {
            return;
        }
        int newCapacity = oldCapacity * 2 + 1;
        Entry newMap[] = new Entry[newCapacity];

        threshold = (int)(newCapacity * loadFactor);
        table = newMap;

        for (i = oldCapacity; --i >= 0 ;) {
            for (Entry old = oldMap[i]; old != null; ) {
                Entry e = old;
                old = old.next;

                int index = (e.oid & 0x7FFFFFFF) % newCapacity;
                e.next = newMap[index];
                newMap[index] = e;
            }
        }
    }

    public synchronized void setDirty(Object obj) {
        int oid = db.getOid(obj);
        Entry tab[] = table;
        int index = (oid & 0x7FFFFFFF) % tab.length;
        nModified += 1;
        for (Entry e = tab[index]; e != null ; e = e.next) {
            if (e.oid == oid) {
                e.dirty += 1;
                return;
            }
        }
    }

    public synchronized void clearDirty(Object obj) {
        int oid = db.getOid(obj);
        Entry tab[] = table;
        int index = (oid & 0x7FFFFFFF) % tab.length;
        for (Entry e = tab[index]; e != null ; e = e.next) {
            if (e.oid == oid) {
                if (e.dirty > 0) { 
                    e.dirty -= 1;
                }
                return;
            }
        }
    }

    public int size() { 
        return count;
    }

    static class Entry { 
        Entry     next;
        Reference ref;
        int       oid;
        int       dirty;
        
        void clear() { 
            ref.clear();
            ref = null;
            dirty = 0;
            next = null;
        }

        Entry(int oid, Reference ref, Entry chain) { 
            next = chain;
            this.oid = oid;
            this.ref = ref;
        }
    }
}

