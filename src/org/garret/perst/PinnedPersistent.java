package org.garret.perst;
import org.garret.perst.impl.StorageImpl;

/**
 * Base class for all persistent capable objects.
 * Unlike Persistent class it doesn't define finalize method.
 * It means that object derived from this class rather than Persistent,
 * can be used either with infinite object cache (DEFAULT_PAGE_POOL_SIZE) either
 * with per-thread serializable transactions (in the last case all modified objects 
 * are pinned in memory until the end of transaction). 
 * Presence of finalize method adds additional memory overhead (especially for small classes)
 * and slow down garbage collections. So using this class instead of Persistent can improve
 * performance of your application.
 */
public class PinnedPersistent implements IPersistent, ICloneable 
{ 
    public synchronized void load() {
        if (oid != 0 && (state & RAW) != 0) { 
            storage.loadObject(this);
        }
    }

    public synchronized void loadAndModify() {
        load();
        modify();
    }

    public final boolean isRaw() { 
        return (state & RAW) != 0;
    } 
    
    public final boolean isModified() { 
        return (state & DIRTY) != 0;
    } 
    
    public final boolean isDeleted() { 
        return (state & DELETED) != 0;
    } 
    
    public final boolean isPersistent() { 
        return oid != 0;
    }
    
    public void makePersistent(Storage storage) { 
        if (oid == 0) { 
            storage.makePersistent(this);
        }
    }

    public void store() {
        if ((state & RAW) != 0) { 
            throw new StorageError(StorageError.ACCESS_TO_STUB);
        }
        if (storage != null) { 
            storage.storeObject(this);
            state &= ~DIRTY;
        }
    }
  
    public void modify() { 
        if ((state & DIRTY) == 0 && oid != 0) { 
            if ((state & RAW) != 0) { 
                throw new StorageError(StorageError.ACCESS_TO_STUB);
            }
            Assert.that((state & DELETED) == 0);
            storage.modifyObject(this);
            state |= DIRTY;
        }
    }

    public PinnedPersistent() {}

    public PinnedPersistent(Storage storage) { 
        this.storage = storage;
    }

    public final int getOid() {
        return oid;
    }

    public void deallocate() { 
        if (oid != 0) { 
            storage.deallocateObject(this);
        }
    }

    public boolean recursiveLoading() {
        return true;
    }
    
    public final Storage getStorage() {
        return storage;
    }
    
    public boolean equals(Object o) { 
        if (o == this) {
            return true;
        } 
        if (oid == 0) { 
            return super.equals(o);
        }
        return o instanceof IPersistent && ((IPersistent)o).getOid() == oid;
    }

    public int hashCode() {
        return oid;
    }

    public void onLoad() {
    }

    public void onStore() {
    }

    public void invalidate() { 
        state &= ~DIRTY;
        state |= RAW;
    }

    transient Storage storage;
    transient int     oid;
    transient int     state;

    static public final int RAW   = 1;
    static public final int DIRTY = 2;
    static public final int DELETED = 4;

    public void unassignOid() {
        oid = 0;
        state = DELETED;
        storage = null;
    }

    public void assignOid(Storage storage, int oid, boolean raw) { 
        this.oid = oid;
        this.storage = storage;
        if (raw) { 
            state |= RAW;
        } else { 
            state &= ~RAW;
        }            
    }

    protected void clearState() { 
        state = 0;
        oid = 0;
    }

    public Object clone() throws CloneNotSupportedException { 
        Persistent p = (Persistent)super.clone();
        p.oid = 0;
        p.state = 0;
        return p;
    }

    public void readExternal(java.io.ObjectInput s) throws java.io.IOException, ClassNotFoundException
    {
        oid = s.readInt();
    }

    public void writeExternal(java.io.ObjectOutput s) throws java.io.IOException
    {
        if (s instanceof StorageImpl.PersistentObjectOutputStream) { 
            makePersistent(((StorageImpl.PersistentObjectOutputStream)s).getStorage());
        }
        s.writeInt(oid);
    }
}





