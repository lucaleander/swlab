package org.garret.perst.impl;
import  org.garret.perst.*;
import  java.lang.ref.*;

public class PinWeakHashTable implements OidHashTable { 
    Entry table[];
    static final float loadFactor = 0.75f;
    int count;
    int threshold;
    int nModified;
    boolean disableRehash;
    StorageImpl db;

    public PinWeakHashTable(StorageImpl db, int initialCapacity) {
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
    
    public synchronized Object get(int oid) {
        Entry tab[] = table;
        int index = (oid & 0x7FFFFFFF) % tab.length;
        for (Entry e = tab[index]; e != null; e = e.next) {
            if (e.oid == oid) {
                if (e.pin != null) { 
                    return e.pin;
                }
                return e.ref.get();
            }
        }
        return null;
    }
    
    public synchronized void reload() {
        disableRehash = true;
        for (int i = 0; i < table.length; i++) { 
            Entry e, next, prev;
            for (e = table[i]; e != null; e = e.next) {                 
                Object obj = e.pin;
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

    public synchronized void flush() {
        disableRehash = true;
        int n;
        do { 
            n = nModified;
            for (int i = 0; i < table.length; i++) { 
                for (Entry e = table[i]; e != null; e = e.next) { 
                    Object obj = e.pin;
                    if (obj != null) {  
                        db.store(obj);
                        e.pin = null;
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

    public synchronized void invalidate() {
        for (int i = 0; i < table.length; i++) { 
            for (Entry e = table[i]; e != null; e = e.next) { 
                Object obj = e.pin;
                if (obj != null) { 
                    e.pin = null;
                    db.invalidate(obj);
                }
            }
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
                if ((obj == null || db.isDeleted(obj)) && e.pin == null) { 
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
                e.pin = obj;
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
                e.pin = null;
                return;
            }
        }
    }

    public int size() { 
        return count;
    }

    static class Entry { 
        Entry       next;
        Reference   ref;
        int         oid;
        Object      pin;
        
        void clear() { 
            ref.clear();
            ref = null;
            pin = null;
            next = null;
        }

        Entry(int oid, Reference ref, Entry chain) { 
            next = chain;
            this.oid = oid;
            this.ref = ref;
        }
    }
}

