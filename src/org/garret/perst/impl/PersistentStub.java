package org.garret.perst.impl;
import org.garret.perst.*;

public class PersistentStub implements IPersistent { 
    public void load() {
        throw new StorageError(StorageError.ACCESS_TO_STUB);
    }

    public void loadAndModify() {
        load();
        modify();
    }

    public final boolean isRaw() { 
        return true;
    } 
    
    public final boolean isModified() { 
        return false;
    } 
    
    public final boolean isDeleted() { 
        return false;
    } 
    
    public final boolean isPersistent() { 
        return true;
    }
    
    public void makePersistent(Storage storage) { 
        throw new StorageError(StorageError.ACCESS_TO_STUB);
    }

    public void store() {
        throw new StorageError(StorageError.ACCESS_TO_STUB);
    }
  
    public void modify() { 
        throw new StorageError(StorageError.ACCESS_TO_STUB);
    }

    public PersistentStub(Storage storage, int oid) { 
        this.storage = storage;
        this.oid = oid;
    }

    public final int getOid() {
        return oid;
    }

    public void deallocate() { 
        throw new StorageError(StorageError.ACCESS_TO_STUB);
    }

    public boolean recursiveLoading() {
        return true;
    }
    
    public final Storage getStorage() {
        return storage;
    }
    
    public boolean equals(Object o) { 
        return getStorage().getOid(o) == oid;
    }

    public int hashCode() {
        return oid;
    }

    public void onLoad() {
    }

    public void onStore() {
    }

    public void invalidate() { 
        throw new StorageError(StorageError.ACCESS_TO_STUB);
    }

    transient Storage storage;
    transient int     oid;

    public void unassignOid() { 
        throw new StorageError(StorageError.ACCESS_TO_STUB);
    }

    public void assignOid(Storage storage, int oid, boolean raw) { 
        throw new StorageError(StorageError.ACCESS_TO_STUB);
    }

    public Object clone() throws CloneNotSupportedException { 
        PersistentStub p = (PersistentStub)super.clone();
        p.oid = 0;
        return p;
    }

    public void readExternal(java.io.ObjectInput s) throws java.io.IOException, ClassNotFoundException
    {
        oid = s.readInt();
    }

    public void writeExternal(java.io.ObjectOutput s) throws java.io.IOException
    {
        s.writeInt(oid);
    }
}





