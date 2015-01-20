package org.garret.perst.impl;
import  org.garret.perst.*;
import  java.util.*;
import  java.lang.reflect.Array;

class Btree<T> extends PersistentCollection<T> implements Index<T> { 
    int       root;
    int       height;
    int       type;
    int       nElems;
    boolean   unique;

    transient int updateCounter;

    static final int sizeof = ObjectHeader.sizeof + 4*4 + 1;

    Btree() {}

    static int checkType(Class c) { 
        int elemType = ClassDescriptor.getTypeCode(c);
        if (elemType > ClassDescriptor.tpObject 
            && elemType != ClassDescriptor.tpEnum 
            && elemType != ClassDescriptor.tpArrayOfByte) 
        { 
            throw new StorageError(StorageError.UNSUPPORTED_INDEX_TYPE, c);
        }
        return elemType;
    }
       
    int compareByteArrays(byte[] key, byte[] item, int offs, int length) { 
        int n = key.length >= length ? length : key.length;
        for (int i = 0; i < n; i++) { 
            int diff = key[i] - item[i+offs];
            if (diff != 0) { 
                return diff;
            }
        }
        return key.length - length;
    }

    Btree(Class cls, boolean unique) {
        this.unique = unique;
        type = checkType(cls);
    }

    Btree(int type, boolean unique) { 
        this.type = type;
        this.unique = unique;
    }

    Btree(byte[] obj, int offs) {
        height = Bytes.unpack4(obj, offs);
        offs += 4;
        nElems = Bytes.unpack4(obj, offs);
        offs += 4;
        root = Bytes.unpack4(obj, offs);
        offs += 4;
        type = Bytes.unpack4(obj, offs);
        offs += 4;
        unique = obj[offs] != 0;
    }

    static final int op_done      = 0;
    static final int op_overflow  = 1;
    static final int op_underflow = 2;
    static final int op_not_found = 3;
    static final int op_duplicate = 4;
    static final int op_overwrite = 5;

    public Class[] getKeyTypes() {
        return new Class[]{getKeyType()};
    }

    public Class getKeyType() {
        return mapKeyType(type);
    }

    static Class mapKeyType(int type) {
        switch (type) { 
        case ClassDescriptor.tpBoolean:
            return boolean.class;
        case ClassDescriptor.tpByte:
            return byte.class;
        case ClassDescriptor.tpChar:
            return char.class;
        case ClassDescriptor.tpShort:
            return short.class;
        case ClassDescriptor.tpInt:
            return int.class;
        case ClassDescriptor.tpLong:
            return long.class;
        case ClassDescriptor.tpFloat:
            return float.class;
        case ClassDescriptor.tpDouble:
            return double.class;
        case ClassDescriptor.tpEnum:
            return Enum.class;
        case ClassDescriptor.tpString:
            return String.class;
        case ClassDescriptor.tpDate:
            return Date.class;
        case ClassDescriptor.tpObject:
            return Object.class;
        case ClassDescriptor.tpArrayOfByte:
            return byte[].class;
        default:
            return Comparable.class;
        }
    }

    Key checkKey(Key key) { 
        if (key != null) { 
            if (key.type != type) { 
                throw new StorageError(StorageError.INCOMPATIBLE_KEY_TYPE);
            }
            if (type == ClassDescriptor.tpObject && key.ival == 0 && key.oval != null) { 
                Object obj = key.oval;
                key = new Key(obj, getStorage().makePersistent(obj), key.inclusion != 0);
            }
            if (key.oval instanceof String) { 
                key = new Key(((String)key.oval).toCharArray(), key.inclusion != 0);
            }
        }
        return key;
    }            

    public T get(Key key) { 
        key = checkKey(key);
        if (root != 0) { 
            ArrayList list = new ArrayList();
            BtreePage.find((StorageImpl)getStorage(), root, key, key, this, height, list);
            if (list.size() > 1) { 
                throw new StorageError(StorageError.KEY_NOT_UNIQUE);
            } else if (list.size() == 0) { 
                return null;
            } else { 
                return (T)list.get(0);
            }
        }
        return null;
    }

    public ArrayList<T> prefixSearchList(String key) { 
        if (ClassDescriptor.tpString != type) { 
            throw new StorageError(StorageError.INCOMPATIBLE_KEY_TYPE);
        }
        ArrayList<T> list = new ArrayList<T>();
        if (root != 0) { 
            BtreePage.prefixSearch((StorageImpl)getStorage(), root, key.toCharArray(), height, list);
        }
        return list;
    }

    public Object[] prefixSearch(String key) {
        ArrayList<T> list = prefixSearchList(key);
        return list.toArray();
    }

    public ArrayList<T> getList(Key from, Key till) {
        ArrayList<T> list = new ArrayList<T>();
        if (root != 0) { 
            BtreePage.find((StorageImpl)getStorage(), root, checkKey(from), checkKey(till), this, height, list);
        }
        return list;
    }

    public ArrayList<T> getList(Object from, Object till) {
        return getList(getKeyFromObject(type, from), getKeyFromObject(type, till));
    }

    public T get(Object key) { 
        return get(getKeyFromObject(type, key));
    }

    public Object[] get(Key from, Key till) {
        ArrayList<T> list = getList(from, till);
        return list.toArray();
    }

    public Object[] get(Object from, Object till) {
        return get(getKeyFromObject(type, from), getKeyFromObject(type, till));
    }

    public boolean put(Key key, T obj) {
        return insert(key, obj, false) >= 0;
    }

    public T set(Key key, T obj) {
        int oid = insert(key, obj, true);
        return (T)((oid != 0) ? ((StorageImpl)getStorage()).lookupObject(oid, null) :  null);
    }

    final int insert(Key key, T obj, boolean overwrite) {
        StorageImpl db = (StorageImpl)getStorage();
        if (db == null) {             
            throw new StorageError(StorageError.DELETED_OBJECT);
        }
        key = checkKey(key);
        BtreeKey ins = new BtreeKey(key, db.makePersistent(obj));
        if (root == 0) { 
            root = BtreePage.allocate(db, 0, type, ins);
            height = 1;
        } else { 
            int result = BtreePage.insert(db, root, this, ins, height, unique, overwrite);
            if (result == op_overflow) { 
                root = BtreePage.allocate(db, root, type, ins);
                height += 1;
            } else if (result == op_duplicate) { 
                return -1;
            } else if (result == op_overwrite) { 
                return ins.oldOid;
            }
        }
        updateCounter += 1;
        nElems += 1;
        modify();
        return 0;
    }

    public void remove(Key key, T obj) {
        remove(new BtreeKey(checkKey(key), getStorage().getOid(obj)));
    }

    public boolean unlink(Key key, T obj) {
        return removeIfExists(key, obj);
    }
    
    boolean removeIfExists(Key key, Object obj) {
        return removeIfExists(new BtreeKey(checkKey(key), getStorage().getOid(obj)));
    }

        
    void remove(BtreeKey rem) 
    {
        if (!removeIfExists(rem)) { 
            throw new StorageError(StorageError.KEY_NOT_FOUND);
        }
    }

    boolean removeIfExists(BtreeKey rem) 
    {            
        StorageImpl db = (StorageImpl)getStorage();
        if (db == null) {             
            throw new StorageError(StorageError.DELETED_OBJECT);
        }
        if (root == 0) {
            return false;
        }
        int result = BtreePage.remove(db, root, this, rem, height);
        if (result == op_not_found) { 
            return false;
        }
        nElems -= 1;
        if (result == op_underflow) { 
            Page pg = db.getPage(root);
            if (BtreePage.getnItems(pg) == 0) {                         
                int newRoot = 0;
                if (height != 1) { 
                    newRoot = (type == ClassDescriptor.tpString || type == ClassDescriptor.tpArrayOfByte) 
                        ? BtreePage.getKeyStrOid(pg, 0)
                        : BtreePage.getReference(pg, BtreePage.maxItems-1);
                }
                db.freePage(root);
                root = newRoot;
                height -= 1;
            }
            db.pool.unfix(pg);
        } else if (result == op_overflow) { 
            root = BtreePage.allocate(db, root, type, rem);
            height += 1;
        }
        updateCounter += 1;
        modify();
        return true;
    }
        
    public T remove(Key key) {
        if (!unique) { 
            throw new StorageError(StorageError.KEY_NOT_UNIQUE);
        }
        BtreeKey rk = new BtreeKey(checkKey(key), 0);
        StorageImpl db = (StorageImpl)getStorage();
        remove(rk);
        return (T)db.lookupObject(rk.oldOid, null);
    }
        
    static Key getKeyFromObject(int type, Object o) {
        if (o == null) { 
            return null;
        }
        switch (type) { 
        case ClassDescriptor.tpBoolean:
            return new Key(((Boolean)o).booleanValue());
        case ClassDescriptor.tpByte:
            return new Key(((Number)o).byteValue());
        case ClassDescriptor.tpChar:
            return new Key(((Character)o).charValue());
        case ClassDescriptor.tpShort:
            return new Key(((Number)o).shortValue());
        case ClassDescriptor.tpInt:
            return new Key(((Number)o).intValue());
        case ClassDescriptor.tpLong:
            return new Key(((Number)o).longValue());
        case ClassDescriptor.tpFloat:
            return new Key(((Number)o).floatValue());
        case ClassDescriptor.tpDouble:
            return new Key(((Number)o).doubleValue());
        case ClassDescriptor.tpString:
            return new Key((String)o);
        case ClassDescriptor.tpDate:
            return new Key((java.util.Date)o);
        case ClassDescriptor.tpObject:
            return new Key(o);
        case ClassDescriptor.tpValue:
            return new Key((IValue)o);
        case ClassDescriptor.tpEnum:
            return new Key((Enum)o);
        case ClassDescriptor.tpArrayOfByte: 
            return new Key((byte[])o);
        default:
            throw new StorageError(StorageError.UNSUPPORTED_INDEX_TYPE);
        }
    }
    static Key getKeyFromObject(Object o) {
        if (o == null) { 
            return null;
        } else if (o instanceof Byte) { 
            return new Key(((Byte)o).byteValue());
        } else if (o instanceof Short) {
            return new Key(((Short)o).shortValue());
        } else if (o instanceof Integer) {
            return new Key(((Integer)o).intValue());
        } else if (o instanceof Long) {
            return new Key(((Long)o).longValue());
        } else if (o instanceof Float) {
            return new Key(((Float)o).floatValue());
        } else if (o instanceof Double) {
            return new Key(((Double)o).doubleValue());
        } else if (o instanceof Boolean) {
            return new Key(((Boolean)o).booleanValue());
        } else if (o instanceof Character) {
            return new Key(((Character)o).charValue());
        } else if (o instanceof String) {
            return new Key((String)o);
        } else if (o instanceof java.util.Date) {
            return new Key((java.util.Date)o);
        } else if (o instanceof byte[]) {
            return new Key((byte[])o);
        } else if (o instanceof Object[]) {
            return new Key((Object[])o);
        } else if (o instanceof Enum) {
            return new Key((Enum)o);
        } else if (o instanceof IValue) {
            return new Key((IValue)o);
        } else {
            return new Key(o);
        }
    }
        

    public ArrayList<T> getPrefixList(String prefix) { 
        return getList(new Key(prefix, true), 
                       new Key(prefix + Character.MAX_VALUE, false));
    }

    public Object[] getPrefix(String prefix) { 
        return get(new Key(prefix, true), 
                   new Key(prefix + Character.MAX_VALUE, false));
    }

    public boolean put(Object key, T obj) {
        return put(getKeyFromObject(type, key), obj);
    }

    public T set(Object key, T obj) {
        return set(getKeyFromObject(type, key), obj);
    }

    public void remove(Object key, T obj) {
        remove(getKeyFromObject(type, key), obj);
    }
    
    public T removeKey(Object key) {
        return remove(getKeyFromObject(type, key));
    }

    public T remove(String key) {
        return remove(new Key(key));
    }

    public int size() {
        return nElems;
    }
    
    public void clear() {
        if (root != 0) { 
            BtreePage.purge((StorageImpl)getStorage(), root, type, height);
            root = 0;
            nElems = 0;
            height = 0;
            updateCounter += 1;
            modify();
        }
    }
        
    public Object[] toArray() {
        Object[] arr = new Object[nElems];
        if (root != 0) { 
            BtreePage.traverseForward((StorageImpl)getStorage(), root, type, height, arr, 0);
        }
        return arr;
    }
  
    public <E> E[] toArray(E[] arr) {
        if (arr.length < nElems) { 
            arr = (E[])Array.newInstance(arr.getClass().getComponentType(), nElems);
        }
        if (root != 0) { 
            BtreePage.traverseForward((StorageImpl)getStorage(), root, type, height, arr, 0);
        }
        if (arr.length > nElems) { 
            arr[nElems] = null;
        }
        return arr;
    }

    public void deallocate() { 
        if (root != 0) { 
            BtreePage.purge((StorageImpl)getStorage(), root, type, height);
        }
        super.deallocate();
    }

    public int markTree() 
    { 
        if (root != 0) { 
            return BtreePage.markPage((StorageImpl)getStorage(), root, type, height);
        }
        return 0;
    }        

    protected Object unpackEnum(int val) 
    {
        // Base B-Tree class has no information about particular enum type
        // so it is not able to correctly unpack enum key
        return (Object)val;
    }

    public void export(XMLExporter exporter) throws java.io.IOException 
    { 
        if (root != 0) { 
            BtreePage.exportPage((StorageImpl)getStorage(), exporter, root, type, height);
        }
    }        

    static class BtreeEntry<T> implements Map.Entry<Object,T> {
        public Object getKey() { 
            return key;
        }

        public T getValue() { 
            return (T)db.lookupObject(oid, null);
        }

        public T setValue(T value) { 
            throw new UnsupportedOperationException();
        }

	public boolean equals(Object o) {
	    if (!(o instanceof Map.Entry)) {
		return false;
            }
	    Map.Entry e = (Map.Entry)o;
	    return (getKey() == null ? e.getKey() == null : getKey().equals(e.getKey())) 
                && (getValue() == null ? e.getValue() == null : getValue().equals(e.getValue())); 
	}

	public int hashCode() {
	    return ((getKey() == null) ? 0 : getKey().hashCode()) ^
                ((getValue() == null) ? 0 : getValue().hashCode());
	}

	public String toString() {
	    return getKey() + "=" + getValue();
	}

        BtreeEntry(StorageImpl db, Object key, int oid) {
            this.db = db;
            this.key = key;
            this.oid = oid;
        }

        private Object      key;
        private StorageImpl db;
        private int         oid;
    }

    Object unpackKey(StorageImpl db, Page pg, int pos) { 
        byte[] data = pg.data;
        int offs =  BtreePage.firstKeyOffs + pos*ClassDescriptor.sizeof[type];
        switch (type) { 
          case ClassDescriptor.tpBoolean:
              return Boolean.valueOf(data[offs] != 0);
          case ClassDescriptor.tpByte:
            return new Byte(data[offs]);
          case ClassDescriptor.tpShort:
            return new Short(Bytes.unpack2(data, offs));
          case ClassDescriptor.tpChar:
            return new Character((char)Bytes.unpack2(data, offs));
          case ClassDescriptor.tpInt:
            return new Integer(Bytes.unpack4(data, offs));
          case ClassDescriptor.tpObject:
            return db.lookupObject(Bytes.unpack4(data, offs), null);
          case ClassDescriptor.tpLong:
            return new Long(Bytes.unpack8(data, offs));
          case ClassDescriptor.tpDate:
            return new Date(Bytes.unpack8(data, offs));
          case ClassDescriptor.tpFloat:
            return new Float(Float.intBitsToFloat(Bytes.unpack4(data, offs)));
          case ClassDescriptor.tpDouble:
            return new Double(Double.longBitsToDouble(Bytes.unpack8(data, offs)));
          case ClassDescriptor.tpEnum:
            return unpackEnum(Bytes.unpack4(data, offs));
          case ClassDescriptor.tpString:
            return unpackStrKey(pg, pos);
          case ClassDescriptor.tpArrayOfByte:
            return unpackByteArrayKey(pg, pos);
          default:
            Assert.failed("Invalid type");
        }
        return null;
    }
    
    static String unpackStrKey(Page pg, int pos) {
        int len = BtreePage.getKeyStrSize(pg, pos);
        int offs = BtreePage.firstKeyOffs + BtreePage.getKeyStrOffs(pg, pos);
        byte[] data = pg.data;
        char[] sval = new char[len];
        for (int j = 0; j < len; j++) { 
            sval[j] = (char)Bytes.unpack2(data, offs);
            offs += 2;
        }
        return new String(sval);
    }
            
    Object unpackByteArrayKey(Page pg, int pos) {
        int len = BtreePage.getKeyStrSize(pg, pos);
        int offs = BtreePage.firstKeyOffs + BtreePage.getKeyStrOffs(pg, pos);
        byte[] val = new byte[len];
        System.arraycopy(pg.data, offs, val, 0, len);
        return val;
    }
                          

    public Iterator<T> iterator() { 
        return iterator(null, null, ASCENT_ORDER);
    }

    public IterableIterator<Map.Entry<Object,T>> entryIterator() { 
        return entryIterator(null, null, ASCENT_ORDER);
    }


    final int compareByteArrays(Key key, Page pg, int i) { 
        return compareByteArrays((byte[])key.oval, 
                                 pg.data, 
                                 BtreePage.getKeyStrOffs(pg, i) + BtreePage.firstKeyOffs, 
                                 BtreePage.getKeyStrSize(pg, i));
    }


    class BtreeSelectionIterator<E> extends IterableIterator<E> implements PersistentIterator { 
        BtreeSelectionIterator(Key from, Key till, int order) { 
            this.from = from;
            this.till = till;
            this.order = order;
            reset();
        }

        void reset() { 
            int i, l, r;
            
            sp = 0;
            counter = updateCounter;
            if (height == 0) { 
                return;
            }
            int pageId = root;
            StorageImpl db = (StorageImpl)getStorage();
            if (db == null) {             
                throw new StorageError(StorageError.DELETED_OBJECT);
            }
            int h = height;
            
            pageStack = new int[h];
            posStack =  new int[h];
            
            if (type == ClassDescriptor.tpString) { 
                if (order == ASCENT_ORDER) { 
                    if (from == null) { 
                        while (--h >= 0) { 
                            posStack[sp] = 0;
                            pageStack[sp] = pageId;
                            Page pg = db.getPage(pageId);
                            pageId = BtreePage.getKeyStrOid(pg, 0);
                            end = BtreePage.getnItems(pg);
                            db.pool.unfix(pg);
                            sp += 1;
                        }
                    } else { 
                        while (--h > 0) { 
                            pageStack[sp] = pageId;
                            Page pg = db.getPage(pageId);
                            l = 0;
                            r = BtreePage.getnItems(pg);
                            while (l < r)  {
                                i = (l+r) >> 1;
                                if (BtreePage.compareStr(from, pg, i) >= from.inclusion) {
                                    l = i + 1; 
                                } else { 
                                    r = i;
                                }
                            }
                            Assert.that(r == l); 
                            posStack[sp] = r;
                            pageId = BtreePage.getKeyStrOid(pg, r);
                            db.pool.unfix(pg);
                            sp += 1;
                        }
                        pageStack[sp] = pageId;
                        Page pg = db.getPage(pageId);
                        l = 0;
                        end = r = BtreePage.getnItems(pg);
                        while (l < r)  {
                            i = (l+r) >> 1;
                            if (BtreePage.compareStr(from, pg, i) >= from.inclusion) {
                                l = i + 1; 
                            } else { 
                                r = i;
                            }
                        }
                        Assert.that(r == l); 
                        if (r == end) {
                            sp += 1;
                            gotoNextItem(pg, r-1);
                        } else { 
                            posStack[sp++] = r;
                            db.pool.unfix(pg);
                        }
                    }
                    if (sp != 0 && till != null) { 
                        Page pg = db.getPage(pageStack[sp-1]);
                        if (-BtreePage.compareStr(till, pg, posStack[sp-1]) >= till.inclusion) { 
                            sp = 0;
                        }
                        db.pool.unfix(pg);
                    }
                } else { // descent order
                    if (till == null) { 
                        while (--h > 0) { 
                            pageStack[sp] = pageId;
                            Page pg = db.getPage(pageId);
                            posStack[sp] = BtreePage.getnItems(pg);
                            pageId = BtreePage.getKeyStrOid(pg, posStack[sp]);
                            db.pool.unfix(pg);
                            sp += 1;
                        }
                        pageStack[sp] = pageId;
                        Page pg = db.getPage(pageId);
                        posStack[sp++] = BtreePage.getnItems(pg)-1;
                        db.pool.unfix(pg);
                    } else {
                        while (--h > 0) { 
                            pageStack[sp] = pageId;
                            Page pg = db.getPage(pageId);
                            l = 0;
                            r = BtreePage.getnItems(pg);
                            while (l < r)  {
                                i = (l+r) >> 1;
                                if (BtreePage.compareStr(till, pg, i) >= 1-till.inclusion) {
                                    l = i + 1; 
                                } else { 
                                    r = i;
                                }
                            }
                            Assert.that(r == l); 
                            posStack[sp] = r;
                            pageId = BtreePage.getKeyStrOid(pg, r);
                            db.pool.unfix(pg);
                            sp += 1;
                        }
                        pageStack[sp] = pageId;
                        Page pg = db.getPage(pageId);
                        l = 0;
                        r = BtreePage.getnItems(pg);
                        while (l < r)  {
                            i = (l+r) >> 1;
                            if (BtreePage.compareStr(till, pg, i) >= 1-till.inclusion) {
                                l = i + 1; 
                            } else { 
                                r = i;
                            }
                        }
                        Assert.that(r == l); 
                        if (r == 0) {
                            sp += 1;
                            gotoNextItem(pg, r);
                        } else { 
                            posStack[sp++] = r-1;
                            db.pool.unfix(pg);
                        }
                    }
                    if (sp != 0 && from != null) { 
                        Page pg = db.getPage(pageStack[sp-1]);
                        if (BtreePage.compareStr(from, pg, posStack[sp-1]) >= from.inclusion) { 
                            sp = 0;
                        }
                        db.pool.unfix(pg);
                    }
                }
            } else if (type == ClassDescriptor.tpArrayOfByte) { 
                if (order == ASCENT_ORDER) { 
                    if (from == null) { 
                        while (--h >= 0) { 
                            posStack[sp] = 0;
                            pageStack[sp] = pageId;
                            Page pg = db.getPage(pageId);
                            pageId = BtreePage.getKeyStrOid(pg, 0);
                            end = BtreePage.getnItems(pg);
                            db.pool.unfix(pg);
                            sp += 1;
                        }
                    } else { 
                        while (--h > 0) { 
                            pageStack[sp] = pageId;
                            Page pg = db.getPage(pageId);
                            l = 0;
                            r = BtreePage.getnItems(pg);
                            while (l < r)  {
                                i = (l+r) >> 1;
                                if (compareByteArrays(from, pg, i) >= from.inclusion) {
                                    l = i + 1; 
                                } else { 
                                    r = i;
                                }
                            }
                            Assert.that(r == l); 
                            posStack[sp] = r;
                            pageId = BtreePage.getKeyStrOid(pg, r);
                            db.pool.unfix(pg);
                            sp += 1;
                        }
                        pageStack[sp] = pageId;
                        Page pg = db.getPage(pageId);
                        l = 0;
                        end = r = BtreePage.getnItems(pg);
                        while (l < r)  {
                            i = (l+r) >> 1;
                            if (compareByteArrays(from, pg, i) >= from.inclusion) {
                                l = i + 1; 
                            } else { 
                                r = i;
                            }
                        }
                        Assert.that(r == l); 
                        if (r == end) {
                            sp += 1;
                            gotoNextItem(pg, r-1);
                        } else { 
                            posStack[sp++] = r;
                            db.pool.unfix(pg);
                        }
                    }
                    if (sp != 0 && till != null) { 
                        Page pg = db.getPage(pageStack[sp-1]);
                        if (-compareByteArrays(till, pg, posStack[sp-1]) >= till.inclusion) { 
                            sp = 0;
                        }
                        db.pool.unfix(pg);
                    }
                } else { // descent order
                    if (till == null) { 
                        while (--h > 0) { 
                            pageStack[sp] = pageId;
                            Page pg = db.getPage(pageId);
                            posStack[sp] = BtreePage.getnItems(pg);
                            pageId = BtreePage.getKeyStrOid(pg, posStack[sp]);
                            db.pool.unfix(pg);
                            sp += 1;
                        }
                        pageStack[sp] = pageId;
                        Page pg = db.getPage(pageId);
                        posStack[sp++] = BtreePage.getnItems(pg)-1;
                        db.pool.unfix(pg);
                    } else {
                        while (--h > 0) { 
                            pageStack[sp] = pageId;
                            Page pg = db.getPage(pageId);
                            l = 0;
                            r = BtreePage.getnItems(pg);
                            while (l < r)  {
                                i = (l+r) >> 1;
                                if (compareByteArrays(till, pg, i) >= 1-till.inclusion) {
                                    l = i + 1; 
                                } else { 
                                    r = i;
                                }
                            }
                            Assert.that(r == l); 
                            posStack[sp] = r;
                            pageId = BtreePage.getKeyStrOid(pg, r);
                            db.pool.unfix(pg);
                            sp += 1;
                        }
                        pageStack[sp] = pageId;
                        Page pg = db.getPage(pageId);
                        l = 0;
                        r = BtreePage.getnItems(pg);
                        while (l < r)  {
                            i = (l+r) >> 1;
                            if (compareByteArrays(till, pg, i) >= 1-till.inclusion) {
                                l = i + 1; 
                            } else { 
                                r = i;
                            }
                        }
                        Assert.that(r == l); 
                        if (r == 0) {
                            sp += 1;
                            gotoNextItem(pg, r);
                        } else { 
                            posStack[sp++] = r-1;
                            db.pool.unfix(pg);
                        }
                    }
                    if (sp != 0 && from != null) { 
                        Page pg = db.getPage(pageStack[sp-1]);
                        if (compareByteArrays(from, pg, posStack[sp-1]) >= from.inclusion) { 
                            sp = 0;
                        }
                        db.pool.unfix(pg);
                    }
                }
            } else { // scalar type
                if (order == ASCENT_ORDER) { 
                    if (from == null) { 
                        while (--h >= 0) { 
                            posStack[sp] = 0;
                            pageStack[sp] = pageId;
                            Page pg = db.getPage(pageId);
                            pageId = BtreePage.getReference(pg, BtreePage.maxItems-1);
                            end = BtreePage.getnItems(pg);
                            db.pool.unfix(pg);
                            sp += 1;
                        }
                    } else { 
                        while (--h > 0) { 
                            pageStack[sp] = pageId;
                            Page pg = db.getPage(pageId);
                            l = 0;
                            r = BtreePage.getnItems(pg);
                            while (l < r)  {
                                i = (l+r) >> 1;
                                if (BtreePage.compare(from, pg, i) >= from.inclusion) {
                                    l = i + 1; 
                                } else { 
                                    r = i;
                                }
                            }
                            Assert.that(r == l); 
                            posStack[sp] = r;
                            pageId = BtreePage.getReference(pg, BtreePage.maxItems-1-r);
                            db.pool.unfix(pg);
                            sp += 1;
                        }
                        pageStack[sp] = pageId;
                        Page pg = db.getPage(pageId);
                        l = 0;
                        r = end = BtreePage.getnItems(pg);
                        while (l < r)  {
                            i = (l+r) >> 1;
                            if (BtreePage.compare(from, pg, i) >= from.inclusion) {
                                l = i + 1; 
                            } else { 
                                r = i;
                            }
                        }
                        Assert.that(r == l); 
                        if (r == end) {
                            sp += 1;
                            gotoNextItem(pg, r-1);
                        } else { 
                            posStack[sp++] = r;
                            db.pool.unfix(pg);
                        }
                    }
                    if (sp != 0 && till != null) { 
                        Page pg = db.getPage(pageStack[sp-1]);
                        if (-BtreePage.compare(till, pg, posStack[sp-1]) >= till.inclusion) { 
                            sp = 0;
                        }
                        db.pool.unfix(pg);
                    }
                } else { // descent order
                    if (till == null) { 
                        while (--h > 0) { 
                            pageStack[sp] = pageId;
                            Page pg = db.getPage(pageId);
                            posStack[sp] = BtreePage.getnItems(pg);
                            pageId = BtreePage.getReference(pg, BtreePage.maxItems-1-posStack[sp]);
                            db.pool.unfix(pg);
                            sp += 1;
                        }
                        pageStack[sp] = pageId;
                        Page pg = db.getPage(pageId);
                        posStack[sp++] = BtreePage.getnItems(pg)-1;
                        db.pool.unfix(pg);
                     } else {
                        while (--h > 0) { 
                            pageStack[sp] = pageId;
                            Page pg = db.getPage(pageId);
                            l = 0;
                            r = BtreePage.getnItems(pg);
                            while (l < r)  {
                                i = (l+r) >> 1;
                                if (BtreePage.compare(till, pg, i) >= 1-till.inclusion) {
                                    l = i + 1; 
                                } else { 
                                    r = i;
                                }
                            }
                            Assert.that(r == l); 
                            posStack[sp] = r;
                            pageId = BtreePage.getReference(pg, BtreePage.maxItems-1-r);
                            db.pool.unfix(pg);
                            sp += 1;
                        }
                        pageStack[sp] = pageId;
                        Page pg = db.getPage(pageId);
                        l = 0;
                        r = BtreePage.getnItems(pg);
                        while (l < r)  {
                            i = (l+r) >> 1;
                            if (BtreePage.compare(till, pg, i) >= 1-till.inclusion) {
                                l = i + 1; 
                            } else { 
                                r = i;
                            }
                        }
                        Assert.that(r == l);  
                        if (r == 0) { 
                            sp += 1;
                            gotoNextItem(pg, r);
                        } else { 
                            posStack[sp++] = r-1;
                            db.pool.unfix(pg);
                        }
                    }
                    if (sp != 0 && from != null) { 
                        Page pg = db.getPage(pageStack[sp-1]);
                        if (BtreePage.compare(from, pg, posStack[sp-1]) >= from.inclusion) { 
                            sp = 0;
                        }
                        db.pool.unfix(pg);
                    }
                }
            }
        }
                

        public boolean hasNext() {
            if (counter != updateCounter) { 
                if (((StorageImpl)getStorage()).concurrentIterator) { 
                    refresh();
                } else { 
                    throw new ConcurrentModificationException();
                }
            }
            return sp != 0;
        }

        public E next() {
            if (!hasNext()) { 
                throw new NoSuchElementException();
            }
            StorageImpl db = (StorageImpl)getStorage();
            int pos = posStack[sp-1];   
            currPos = pos;
            currPage = pageStack[sp-1];
            Page pg = db.getPage(currPage);
            E curr = (E)getCurrent(pg, pos);
            if (db.concurrentIterator) { 
                currKey = getCurrentKey(pg, pos);
            }
            gotoNextItem(pg, pos);
            return curr;
        }


        public int nextOid() {
           if (!hasNext()) { 
               return 0;                   
            }
            StorageImpl db = (StorageImpl)getStorage();
            int pos = posStack[sp-1];   
            currPos = pos;
            currPage = pageStack[sp-1];
            Page pg = db.getPage(currPage);
            int oid = getReference(pg, pos);
            if (db.concurrentIterator) { 
                currKey = getCurrentKey(pg, pos);
            }
            gotoNextItem(pg, pos);
            return oid;
        }

        private int getReference(Page pg, int pos) { 
            return (type == ClassDescriptor.tpString || type == ClassDescriptor.tpArrayOfByte)
                ? BtreePage.getKeyStrOid(pg, pos)
                : BtreePage.getReference(pg, BtreePage.maxItems-1-pos);
        }


        protected Object getCurrent(Page pg, int pos) { 
            StorageImpl db = (StorageImpl)getStorage();
            return db.lookupObject(getReference(pg, pos), null);
        }

        protected final void gotoNextItem(Page pg, int pos)
        {
            StorageImpl db = (StorageImpl)getStorage();
            if (type == ClassDescriptor.tpString) { 
                if (order == ASCENT_ORDER) {                     
                    if (++pos == end) { 
                        while (--sp != 0) { 
                            db.pool.unfix(pg);
                            pos = posStack[sp-1];
                            pg = db.getPage(pageStack[sp-1]);
                            if (++pos <= BtreePage.getnItems(pg)) {
                                posStack[sp-1] = pos;
                                do { 
                                    int pageId = BtreePage.getKeyStrOid(pg, pos);
                                    db.pool.unfix(pg);
                                    pg = db.getPage(pageId);
                                    end = BtreePage.getnItems(pg);
                                    pageStack[sp] = pageId;
                                    posStack[sp] = pos = 0;
                                } while (++sp < pageStack.length);
                                break;
                            }
                        }
                    } else { 
                        posStack[sp-1] = pos;
                    }
                    if (sp != 0 && till != null && -BtreePage.compareStr(till, pg, pos) >= till.inclusion) { 
                        sp = 0;
                    }
                } else { // descent order
                    if (--pos < 0) { 
                        while (--sp != 0) { 
                            db.pool.unfix(pg);
                            pos = posStack[sp-1];
                            pg = db.getPage(pageStack[sp-1]);
                            if (--pos >= 0) {
                                posStack[sp-1] = pos;
                                do { 
                                    int pageId = BtreePage.getKeyStrOid(pg, pos);
                                    db.pool.unfix(pg);
                                    pg = db.getPage(pageId);
                                    pageStack[sp] = pageId;
                                    posStack[sp] = pos = BtreePage.getnItems(pg);
                                } while (++sp < pageStack.length);
                                posStack[sp-1] = --pos;
                                break;
                            }
                        }
                    } else { 
                        posStack[sp-1] = pos;
                    }
                    if (sp != 0 && from != null && BtreePage.compareStr(from, pg, pos) >= from.inclusion) { 
                        sp = 0;
                    }                    
                }
            } else if (type == ClassDescriptor.tpArrayOfByte) { 
                if (order == ASCENT_ORDER) {                     
                    if (++pos == end) { 
                        while (--sp != 0) { 
                            db.pool.unfix(pg);
                            pos = posStack[sp-1];
                            pg = db.getPage(pageStack[sp-1]);
                            if (++pos <= BtreePage.getnItems(pg)) {
                                posStack[sp-1] = pos;
                                do { 
                                    int pageId = BtreePage.getKeyStrOid(pg, pos);
                                    db.pool.unfix(pg);
                                    pg = db.getPage(pageId);
                                    end = BtreePage.getnItems(pg);
                                    pageStack[sp] = pageId;
                                    posStack[sp] = pos = 0;
                                } while (++sp < pageStack.length);
                                break;
                            }
                        }
                    } else { 
                        posStack[sp-1] = pos;
                    }
                    if (sp != 0 && till != null && -compareByteArrays(till, pg, pos) >= till.inclusion) { 
                        sp = 0;
                    }
                } else { // descent order
                    if (--pos < 0) { 
                        while (--sp != 0) { 
                            db.pool.unfix(pg);
                            pos = posStack[sp-1];
                            pg = db.getPage(pageStack[sp-1]);
                            if (--pos >= 0) {
                                posStack[sp-1] = pos;
                                do { 
                                    int pageId = BtreePage.getKeyStrOid(pg, pos);
                                    db.pool.unfix(pg);
                                    pg = db.getPage(pageId);
                                    pageStack[sp] = pageId;
                                    posStack[sp] = pos = BtreePage.getnItems(pg);
                                } while (++sp < pageStack.length);
                                posStack[sp-1] = --pos;
                                break;
                            }
                        }
                    } else { 
                        posStack[sp-1] = pos;
                    }
                    if (sp != 0 && from != null && compareByteArrays(from, pg, pos) >= from.inclusion) { 
                        sp = 0;
                    }                    
                }
            } else { // scalar type
                if (order == ASCENT_ORDER) {                     
                    if (++pos == end) { 
                        while (--sp != 0) { 
                            db.pool.unfix(pg);
                            pos = posStack[sp-1];
                            pg = db.getPage(pageStack[sp-1]);
                            if (++pos <= BtreePage.getnItems(pg)) {
                                posStack[sp-1] = pos;
                                do { 
                                    int pageId = BtreePage.getReference(pg, BtreePage.maxItems-1-pos);
                                    db.pool.unfix(pg);
                                    pg = db.getPage(pageId);
                                    end = BtreePage.getnItems(pg);
                                    pageStack[sp] = pageId;
                                    posStack[sp] = pos = 0;
                                } while (++sp < pageStack.length);
                                break;
                            }
                        }
                    } else { 
                        posStack[sp-1] = pos;
                    }
                    if (sp != 0 && till != null && -BtreePage.compare(till, pg, pos) >= till.inclusion) { 
                        sp = 0;
                    }
                } else { // descent order
                    if (--pos < 0) { 
                        while (--sp != 0) { 
                            db.pool.unfix(pg);
                            pos = posStack[sp-1];
                            pg = db.getPage(pageStack[sp-1]);
                            if (--pos >= 0) {
                                posStack[sp-1] = pos;
                                do { 
                                    int pageId = BtreePage.getReference(pg, BtreePage.maxItems-1-pos);
                                    db.pool.unfix(pg);
                                    pg = db.getPage(pageId);
                                    pageStack[sp] = pageId;
                                    posStack[sp] = pos = BtreePage.getnItems(pg);
                                } while (++sp < pageStack.length);
                                posStack[sp-1] = --pos;
                                break;
                            }
                        }
                    } else { 
                        posStack[sp-1] = pos;
                    }
                    if (sp != 0 && from != null && BtreePage.compare(from, pg, pos) >= from.inclusion) { 
                        sp = 0;
                    }                    
                }
            }
            if (db.concurrentIterator && sp != 0) { 
                nextKey = getCurrentKey(pg, pos);
            }
            db.pool.unfix(pg);
        }

        private void refresh() { 
            if (sp != 0) { 
                if (nextKey == null) { 
                    reset();
                } else { 
                    if (order == ASCENT_ORDER) { 
                        from = nextKey.key;
                    } else { 
                        till = nextKey.key;
                    }
                    int next = nextKey.oid;
                    reset();
                    StorageImpl db = (StorageImpl)getStorage();
                    while (true) { 
                        int pos = posStack[sp-1];   
                        Page pg = db.getPage(pageStack[sp-1]);
                        int oid = type == ClassDescriptor.tpString || type == ClassDescriptor.tpArrayOfByte
                            ? BtreePage.getKeyStrOid(pg, pos)
                            : BtreePage.getReference(pg, BtreePage.maxItems-1-pos);
                        if (oid != next) { 
                            gotoNextItem(pg, pos);
                        } else { 
                            db.pool.unfix(pg);
                            break;
                        }
                    }
                }
            }
            counter = updateCounter;
        }
            
        BtreeKey getCurrentKey(Page pg, int pos) { 
            BtreeKey key;
            switch (type) { 
            case ClassDescriptor.tpString:
                key = new BtreeKey(null, BtreePage.getKeyStrOid(pg, pos));
                key.getStr(pg, pos);
                break;
            case ClassDescriptor.tpArrayOfByte:
                key = new BtreeKey(null, BtreePage.getKeyStrOid(pg, pos));
                key.getByteArray(pg, pos);
                break;
            default:
                key = new BtreeKey(null, BtreePage.getReference(pg, BtreePage.maxItems-1-pos));
                key.extract(pg, BtreePage.firstKeyOffs + pos*ClassDescriptor.sizeof[type], type);
            }
            return key;
        }

        public void remove() { 
            if (currPage == 0) { 
                throw new NoSuchElementException();
            }
            StorageImpl db = (StorageImpl)getStorage();
            if (!db.concurrentIterator) { 
                if (counter != updateCounter) { 
                    throw new ConcurrentModificationException();
                }
                Page pg = db.getPage(currPage);
                currKey = getCurrentKey(pg, currPos);
                db.pool.unfix(pg);
                if (sp != 0) { 
                    int pos = posStack[sp-1];   
                    pg = db.getPage(pageStack[sp-1]);
                    nextKey = getCurrentKey(pg, pos);
                    db.pool.unfix(pg);
                }
                // System.out.println("Deleted key=" + deletedKey.key.ival + ", next key=" + nextKey.key.ival);
            }
            Btree.this.removeIfExists(currKey);
            refresh();
            currPage = 0;
        }

        int[]       pageStack;
        int[]       posStack;
        int         currPage;
        int         currPos;
        int         sp;
        int         end;
        Key         from;
        Key         till;
        int         order;
        int         counter;
        BtreeKey    nextKey;
        BtreeKey    currKey;
    }

    class BtreeSelectionEntryIterator extends BtreeSelectionIterator<Map.Entry<Object,T>> { 
        BtreeSelectionEntryIterator(Key from, Key till, int order) {
            super(from, till, order);
        }
            
        protected Object getCurrent(Page pg, int pos) { 
            StorageImpl db = (StorageImpl)getStorage();
            switch (type) { 
              case ClassDescriptor.tpString:
                return new BtreeEntry<T>(db, unpackStrKey(pg, pos), BtreePage.getKeyStrOid(pg, pos));
              case ClassDescriptor.tpArrayOfByte:
                return new BtreeEntry<T>(db, unpackByteArrayKey(pg, pos), BtreePage.getKeyStrOid(pg, pos));
              default:
                return new BtreeEntry<T>(db, unpackKey(db, pg, pos), BtreePage.getReference(pg, BtreePage.maxItems-1-pos));
            }
        }
    }

    class BtreeEntryStartFromIterator extends BtreeSelectionEntryIterator
    { 
        BtreeEntryStartFromIterator(int start, int order) {
            super(null, null, order);
            this.start = start;
            reset();
        }
        
        void reset() { 
            super.reset();
            int skip = (order == ASCENT_ORDER) ? start : nElems - start - 1;
            while (--skip >= 0 && hasNext()) {
                next();
            }
        }
        
        int start;
    }
 
    public IterableIterator<T> iterator(Key from, Key till, int order) { 
        return new BtreeSelectionIterator<T>(checkKey(from), checkKey(till), order);
    }

    public IterableIterator<T> prefixIterator(String prefix) {
        return prefixIterator(prefix, ASCENT_ORDER);
    }

    public IterableIterator<T> prefixIterator(String prefix, int order) {
        return iterator(new Key(prefix), 
                        new Key(prefix + Character.MAX_VALUE, false), order);
    }


    public IterableIterator<Map.Entry<Object,T>> entryIterator(Key from, Key till, int order) { 
        return new BtreeSelectionEntryIterator(checkKey(from), checkKey(till), order);
    }


    public IterableIterator<T> iterator(Object from, Object till, int order) { 
        return new BtreeSelectionIterator<T>(checkKey(getKeyFromObject(type, from)), 
                                             checkKey(getKeyFromObject(type, till)), order);
    }

    public IterableIterator<Map.Entry<Object,T>> entryIterator(Object from, Object till, int order) { 
        return new BtreeSelectionEntryIterator(checkKey(getKeyFromObject(type, from)), 
                                               checkKey(getKeyFromObject(type, till)), order);
    }

    public int indexOf(Key key) { 
        PersistentIterator iterator = (PersistentIterator)iterator(null, key, DESCENT_ORDER);
        int i;
        for (i = -1; iterator.nextOid() != 0; i++);
        return i;
    }

    public T getAt(int i) {
        IterableIterator<Map.Entry<Object,T>> iterator;
        if (i < 0 || i >= nElems) {
            throw new IndexOutOfBoundsException("Position " + i + ", index size "  + nElems);
        }            
        if (i <= (nElems/2)) {
            iterator = entryIterator(null, null, ASCENT_ORDER);
            while (--i >= 0) { 
                iterator.next();
            }
        } else {
            iterator = entryIterator(null, null, DESCENT_ORDER);
            i -= nElems;
            while (++i < 0) { 
                iterator.next();
            }
        }
        return iterator.next().getValue();   
    }

    public IterableIterator<Map.Entry<Object,T>> entryIterator(int start, int order) {
        return new BtreeEntryStartFromIterator(start, order);
    }

    public boolean isUnique() {
        return unique;
    }
}

