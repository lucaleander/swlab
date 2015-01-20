package org.garret.perst.impl;
import  org.garret.perst.*;
import  java.util.*;
import  java.lang.reflect.Array;

class RndBtree<T> extends PersistentCollection<T> implements Index<T> { 
    int       height;
    int       type;
    int       nElems;
    boolean   unique;
    BtreePage root;

    transient int updateCounter;

    RndBtree() {}

    static class BtreeKey { 
        Key    key;
        Object node;
        Object oldNode;

        BtreeKey(Key key, Object node) { 
            this.key = key;
            this.node = node;
        }
    }

    static abstract class BtreePage extends Persistent { 
        int  nItems;
        Link items;
        int[] nChildren;

        static final int BTREE_PAGE_SIZE = Page.pageSize - ObjectHeader.sizeof - 4*4;

        abstract Object    getData();
        abstract Object    getKeyValue(int i);
        abstract Key       getKey(int i);
        abstract int       compare(Key key, int i);            
        abstract void      insert(BtreeKey key, int i);
        abstract BtreePage clonePage();
        
        void clearKeyValue(int i) {}

        Object getAt(int i, int height) { 
            if (--height == 0) {
                return items.get(i);
            } else { 
                int j;
                for (j = 0; i >= nChildren[j]; j++) {
                    i -= nChildren[j];
                }
                return ((BtreePage)items.get(j)).getAt(i, height);
            }
        }
        
        int indexOf(Key key, int height) { 
            int l = 0, n = nItems, r = n;
            height -= 1;
            while (l < r)  {
                int i = (l+r) >> 1;
                if (compare(key, i) > 0) {
                    l = i+1;
                } else {
                    r = i;
                }
            }
            Assert.that(r == l);
            if (height == 0) {
                return (r < n && compare(key, r) == 0) ? r : -1;
            } else { 
                int pos = ((BtreePage)items.get(r)).indexOf(key, height);
                if (pos >= 0) { 
                    while (--r >= 0) { 
                        pos += nChildren[r];
                    }
                }
                return pos;
            }
        }            

        boolean find(Key firstKey, Key lastKey, int height, ArrayList result)
        {
            int l = 0, n = nItems, r = n;
            height -= 1;
            if (firstKey != null) {
                while (l < r)  {
                    int i = (l+r) >> 1;
                    if (compare(firstKey, i) >= firstKey.inclusion) {
                        l = i+1;
                    } else {
                        r = i;
                    }
                }
                Assert.that(r == l);
            }
            if (lastKey != null) {
                if (height == 0) {
                    while (l < n) {
                        if (-compare(lastKey, l) >= lastKey.inclusion) {
                            return false;
                        }
                        result.add(items.get(l));
                        l += 1;
                    }
                    return true;
                } else {
                    do {
                        if (!((BtreePage)items.get(l)).find(firstKey, lastKey, height, result)) {
                            return false;
                        }
                        if (l == n) {
                            return true;
                        }
                    } while (compare(lastKey, l++) >= 0);
                    return false;
                }
            } 
            if (height == 0) { 
                while (l < n) { 
                    result.add(items.get(l));
                    l += 1;
                }
            } else { 
                do {
                    if (!((BtreePage)items.get(l)).find(firstKey, lastKey, height, result)) {
                        return false;
                    }
                } while (++l <= n);
            }
            return true;
        }

        static void memcpyData(BtreePage dst_pg, int dst_idx, BtreePage src_pg, int src_idx, int len) 
        { 
            System.arraycopy(src_pg.getData(), src_idx, dst_pg.getData(), dst_idx, len);
        }

        static void memcpyItems(BtreePage dst_pg, int dst_idx, BtreePage src_pg, int src_idx, int len) 
        { 
            System.arraycopy(src_pg.items.toRawArray(), src_idx, dst_pg.items.toRawArray(), dst_idx, len);            
            System.arraycopy(src_pg.nChildren, src_idx, dst_pg.nChildren, dst_idx, len);            
        }

        static void memcpy(BtreePage dst_pg, int dst_idx, BtreePage src_pg, int src_idx, int len) 
        { 
            memcpyData(dst_pg, dst_idx, src_pg, src_idx, len);
            memcpyItems(dst_pg, dst_idx, src_pg, src_idx, len);
        }

        void memset(int i, int len) { 
            while (--len >= 0) { 
                items.setObject(i++, null);
            }
        }

        private void countChildren(int i, int height) {
            nChildren[i] = ((BtreePage)items.get(i)).totalCount(height);
        }

        private int totalCount(int height) { 
            if (--height == 0) { 
                return nItems;
            } else { 
                int sum = 0;
                for (int i = nItems; i >= 0; i--) { 
                    sum += nChildren[i];
                }
                return sum;
            }
        }

        private void insert(BtreeKey key, int i, int height) {
            insert(key, i);
            if (height != 0) {
                countChildren(i, height);
            }
        }

        int insert(BtreeKey ins, int height, boolean unique, boolean overwrite)
        {
            int result;
            int l = 0, n = nItems, r = n;
            int ahead = unique ? 1 : 0;
            while (l < r)  {
                int i = (l+r) >> 1;
                if (compare(ins.key, i) >= ahead) { 
                    l = i+1; 
                } else { 
                    r = i;
                }
            }
            Assert.that(l == r);
            /* insert before e[r] */
            if (--height != 0) {
                result = ((BtreePage)items.get(r)).insert(ins, height, unique, overwrite);
                Assert.that(result != op_not_found);
                if (result != op_overflow) {
                    if (result == op_done) {
                        modify();
                        nChildren[r] += 1;
                    }
                    return result;
                }
                n += 1;
            } else if (r < n && compare(ins.key, r) == 0) { 
                if (overwrite) { 
                    ins.oldNode = items.get(r);
                    modify();
                    items.setObject(r, ins.node);
                    return op_overwrite;
                } else if (unique) { 
                    ins.oldNode = items.get(r);
                    return op_duplicate;
                }
            }
            int max = items.size();
            modify();
            if (height != 0) {
                countChildren(r, height);
            }
            if (n < max) {
                memcpy(this, r+1, this, r, n - r);
                insert(ins, r, height);
                nItems += 1;
                return op_done;
            } else { /* page is full then divide page */
                BtreePage b = clonePage();
                Assert.that(n == max);
                int m = (max+1)/2;
                if (r < m) {
                    memcpy(b, 0, this, 0, r);
                    memcpy(b, r+1, this, r, m-r-1);
                    memcpy(this, 0, this, m-1, max-m+1);
                    b.insert(ins, r, height);
                } else {
                    memcpy(b, 0, this, 0, m);
                    memcpy(this, 0, this, m, r-m);
                    memcpy(this, r-m+1, this, r, max-r);
                    insert(ins, r-m, height);
                }
                memset(max-m+1, m-1);
                ins.node = b;
                ins.key = b.getKey(m-1);
                if (height == 0) {
                    nItems = max - m + 1;
                    b.nItems = m;
                } else {
                    b.clearKeyValue(m-1);
                    nItems = max - m;
                    b.nItems = m - 1;
                }                            
                return op_overflow;
            }
        }

        int handlePageUnderflow(int r, BtreeKey rem, int height)
        {
            BtreePage a = (BtreePage)items.get(r);
            a.modify();
            modify();
            int an = a.nItems;
            if (r < nItems) { // exists greater page
                BtreePage b = (BtreePage)items.get(r+1);
                int bn = b.nItems; 
                Assert.that(bn >= an);
                if (height != 1) { 
                    memcpyData(a, an, this, r, 1);
                    an += 1;
                    bn += 1;
                }
                if (an + bn > items.size()) { 
                    // reallocation of nodes between pages a and b
                    int i = bn - ((an + bn) >> 1);
                    b.modify();
                    memcpy(a, an, b, 0, i);
                    memcpy(b, 0, b, i, bn-i);
                    memcpyData(this, r, a, an+i-1, 1);
                    if (height != 1) { 
                        a.clearKeyValue(an+i-1);
                    }
                    b.memset(bn-i, i);
                    b.nItems -= i;
                    a.nItems += i;
                    countChildren(r, height);
                    countChildren(r+1, height);
                    return op_done;
                } else { // merge page b to a  
                    memcpy(a, an, b, 0, bn);
                    b.deallocate();
                    int nMergedChildren = nChildren[r+1];
                    memcpyData(this, r, this, r+1, nItems - r - 1);
                    memcpyItems(this, r+1, this, r+2, nItems - r - 1);
                    items.setObject(nItems, null);
                    a.nItems += bn;
                    nItems -= 1;
                    nChildren[r] += nMergedChildren-1;
                    return nItems < items.size()/3 ? op_underflow : op_done;
                }
            } else { // page b is before a
                BtreePage b = (BtreePage)items.get(r-1);
                int bn = b.nItems; 
                Assert.that(bn >= an);
                if (height != 1) { 
                    an += 1;
                    bn += 1;
                }
                if (an + bn > items.size()) { 
                    // reallocation of nodes between pages a and b
                    int i = bn - ((an + bn) >> 1);
                    b.modify();
                    memcpy(a, i, a, 0, an);
                    memcpy(a, 0, b, bn-i, i);
                    if (height != 1) { 
                        memcpyData(a, i-1, this, r-1, 1);
                    }
                    memcpyData(this, r-1, b, bn-i-1, 1);
                    if (height != 1) { 
                        b.clearKeyValue(bn-i-1);
                    }
                    b.memset(bn-i, i);
                    b.nItems -= i;
                    a.nItems += i;
                    countChildren(r-1, height);
                    countChildren(r, height);
                    return op_done;
                } else { // merge page b to a
                    memcpy(a, bn, a, 0, an);
                    memcpy(a, 0, b, 0, bn);
                    if (height != 1) { 
                        memcpyData(a, bn-1, this, r-1, 1);
                    }
                    b.deallocate();
                    items.setObject(r-1, a);
                    items.setObject(nItems, null);
                    nChildren[r-1] += nChildren[r] - 1;
                    a.nItems += bn;
                    nItems -= 1;
                    return nItems < items.size()/3 ? op_underflow : op_done;
                }
            }
        }
   
        int remove(BtreeKey rem, int height)
        {
            int i, n = nItems, l = 0, r = n;
            
            while (l < r)  {
                i = (l+r) >> 1;
                if (compare(rem.key, i) > 0) { 
                    l = i+1; 
                } else { 
                    r = i;
                }
            }
            if (--height == 0) {
                Object node = rem.node;
                while (r < n) {
                    if (compare(rem.key, r) == 0) {
                        if (node == null || items.containsElement(r, node)) {
                            rem.oldNode = items.get(r);
                            modify();
                            memcpy(this, r, this, r+1, n - r - 1);
                            nItems = --n;
                            memset(n, 1);
                            return n < items.size()/3 ? op_underflow : op_done;
                        }
                    } else {
                        break;
                    }
                    r += 1;
                }
                return op_not_found;
            }
            do { 
                switch (((BtreePage)items.get(r)).remove(rem, height)) {
                case op_underflow: 
                    return handlePageUnderflow(r, rem, height);
                case op_done:
                    modify();
                    nChildren[r] -= 1;
                    return op_done;
                } 
            } while (++r <= n);
            
            return op_not_found;
        }
        
        void purge(int height)
        {
            if (--height != 0) { 
                int n = nItems;
                do { 
                    ((BtreePage)items.get(n)).purge(height);
                } while (--n >= 0);
            }
            super.deallocate();
        }

        int traverseForward(int height, Object[] result, int pos)
        {
            int i, n = nItems;
            if (--height != 0) {
                for (i = 0; i <= n; i++) { 
                    pos = ((BtreePage)items.get(i)).traverseForward(height, result, pos);
                }
            } else { 
                for (i = 0; i < n; i++) { 
                    result[pos++] = items.get(i);
                }
            }
            return pos;
        }

        BtreePage(Storage s, int n) 
        { 
            super(s);
            items = s.createLink(n);
            items.setSize(n);
            nChildren = new int[n];
        }

        BtreePage() {}            
    }


    static class BtreePageOfByte extends BtreePage { 
        byte[] data; 

        static final int MAX_ITEMS = BTREE_PAGE_SIZE / (4 + 4 + 1);
            
        Object getData() { 
            return data;
        }

        Object getKeyValue(int i) { 
            return new Byte(data[i]);
        }

        Key getKey(int i) { 
            return new Key(data[i]);
        }

        BtreePage clonePage() { 
            return new BtreePageOfByte(getStorage());
        }

        int compare(Key key, int i) {
            return (byte)key.ival - data[i];
        }

        void insert(BtreeKey key, int i) { 
            items.setObject(i, key.node);
            data[i] = (byte)key.key.ival;
        }

        BtreePageOfByte(Storage s) {
            super(s, MAX_ITEMS);
            data = new byte[MAX_ITEMS];
        }

        BtreePageOfByte() {}
    }

    static class BtreePageOfBoolean extends BtreePageOfByte { 
        Key getKey(int i) { 
            return new Key(data[i] != 0);
        }

        Object getKeyValue(int i) { 
            return Boolean.valueOf(data[i] != 0);
        }

        BtreePage clonePage() { 
            return new BtreePageOfBoolean(getStorage());
        }

        BtreePageOfBoolean() {}
        
        BtreePageOfBoolean(Storage s) {
            super(s);
        }        
    }

    static class BtreePageOfShort extends BtreePage { 
        short[] data; 

        static final int MAX_ITEMS = BTREE_PAGE_SIZE / (4 + 4 + 2);
            
        Object getData() { 
            return data;
        }

        Key getKey(int i) { 
            return new Key(data[i]);
        }

        Object getKeyValue(int i) { 
            return new Short(data[i]);
        }

        BtreePage clonePage() { 
            return new BtreePageOfShort(getStorage());
        }

        int compare(Key key, int i) {
            return (short)key.ival - data[i];
        }

        void insert(BtreeKey key, int i) { 
            items.setObject(i, key.node);
            data[i] = (short)key.key.ival;
        }

        BtreePageOfShort(Storage s) {
            super(s, MAX_ITEMS);
            data = new short[MAX_ITEMS];
        }

        BtreePageOfShort() {}
    }

    static class BtreePageOfChar extends BtreePage { 
        char[] data; 

        static final int MAX_ITEMS = BTREE_PAGE_SIZE / (4 + 4 + 2);
            
        Object getData() { 
            return data;
        }

        Key getKey(int i) { 
            return new Key(data[i]);
        }

        Object getKeyValue(int i) { 
            return new Character(data[i]);
        }

        BtreePage clonePage() { 
            return new BtreePageOfChar(getStorage());
        }

        int compare(Key key, int i) {
            return (char)key.ival - data[i];
        }

        void insert(BtreeKey key, int i) { 
            items.setObject(i, key.node);
            data[i] = (char)key.key.ival;
        }

        BtreePageOfChar(Storage s) {
            super(s, MAX_ITEMS);
            data = new char[MAX_ITEMS];
        }

        BtreePageOfChar() {}
    }

    static class BtreePageOfInt extends BtreePage { 
        int[] data; 

        static final int MAX_ITEMS = BTREE_PAGE_SIZE / (4 + 4 + 4);
            
        Object getData() { 
            return data;
        }

        Key getKey(int i) { 
            return new Key(data[i]);
        }

        Object getKeyValue(int i) { 
            return new Integer(data[i]);
        }

        BtreePage clonePage() { 
            return new BtreePageOfInt(getStorage());
        }

        int compare(Key key, int i) {
            return key.ival < data[i] ? -1 : key.ival == data[i] ? 0 : 1;
        }

        void insert(BtreeKey key, int i) { 
            items.setObject(i, key.node);
            data[i] = key.key.ival;
        }

        BtreePageOfInt(Storage s) { 
            super(s, MAX_ITEMS);
            data = new int[MAX_ITEMS];
        }

        BtreePageOfInt() {}
    }

    static class BtreePageOfLong extends BtreePage { 
        long[] data; 

        static final int MAX_ITEMS = BTREE_PAGE_SIZE / (4 + 4 + 8);
            
        Object getData() { 
            return data;
        }

        Key getKey(int i) { 
            return new Key(data[i]);
        }

        Object getKeyValue(int i) { 
            return new Long(data[i]);
        }

        BtreePage clonePage() { 
            return new BtreePageOfLong(getStorage());
        }

        int compare(Key key, int i) {
            return key.lval < data[i] ?  -1 : key.lval == data[i] ? 0 : 1;
        }

        void insert(BtreeKey key, int i) { 
            items.setObject(i, key.node);
            data[i] = key.key.lval;
        }

        BtreePageOfLong(Storage s) { 
            super(s, MAX_ITEMS);
            data = new long[MAX_ITEMS];
        }

        BtreePageOfLong() {}
    }

    static class BtreePageOfFloat extends BtreePage { 
        float[] data; 

        static final int MAX_ITEMS = BTREE_PAGE_SIZE / (4 + 4 + 4);
            
        Object getData() { 
            return data;
        }

        Key getKey(int i) { 
            return new Key(data[i]);
        }

        Object getKeyValue(int i) { 
            return new Float(data[i]);
        }

        BtreePage clonePage() { 
            return new BtreePageOfFloat(getStorage());
        }

        int compare(Key key, int i) {
            return (float)key.dval < data[i] ? -1 : (float)key.dval == data[i] ? 0 : 1;
        }

        void insert(BtreeKey key, int i) { 
            items.setObject(i, key.node);
            data[i] = (float)key.key.dval;
        }

        BtreePageOfFloat(Storage s) {
            super(s, MAX_ITEMS);
            data = new float[MAX_ITEMS];
        }

        BtreePageOfFloat() {}
    }

    static class BtreePageOfDouble extends BtreePage { 
        double[] data; 

        static final int MAX_ITEMS = BTREE_PAGE_SIZE / (4 + 4 + 8);
            
        Object getData() { 
            return data;
        }

        Key getKey(int i) { 
            return new Key(data[i]);
        }

        Object getKeyValue(int i) { 
            return new Double(data[i]);
        }

        BtreePage clonePage() { 
            return new BtreePageOfDouble(getStorage());
        }

        int compare(Key key, int i) {
            return key.dval < data[i] ? -1 : key.dval == data[i] ? 0 : 1;
        }

        void insert(BtreeKey key, int i) { 
            items.setObject(i, key.node);
            data[i] = key.key.dval;
        }

        BtreePageOfDouble(Storage s) {
            super(s, MAX_ITEMS);
            data = new double[MAX_ITEMS];
        }

        BtreePageOfDouble() {}
    }


    static class BtreePageOfObject extends BtreePage { 
        Link data; 

        static final int MAX_ITEMS = BTREE_PAGE_SIZE / (4 + 4 + 4);
            
        Object getData() { 
            return data.toRawArray();
        }

        Key getKey(int i) { 
            return new Key(data.getRaw(i));
        }

        Object getKeyValue(int i) { 
            return data.get(i);
        }

        BtreePage clonePage() { 
            return new BtreePageOfObject(getStorage());
        }

        int compare(Key key, int i) {
            Object obj = data.getRaw(i);
            int oid = getStorage().getOid(obj);
            return key.ival < oid ? -1 : key.ival == oid ? 0 : 1;
        }

        void insert(BtreeKey key, int i) { 
            items.setObject(i, key.node);
            data.setObject(i, key.key.oval);
        }

        BtreePageOfObject(Storage s) {
            super(s, MAX_ITEMS);
            data = s.createLink(MAX_ITEMS);
            data.setSize(MAX_ITEMS);
        }

        BtreePageOfObject() {}
    }

    static class BtreePageOfString extends BtreePage { 
        String[] data; 

        static final int MAX_ITEMS = 100;
            
        Object getData() { 
            return data;
        }

        Key getKey(int i) { 
            return new Key(data[i]);
        }

        Object getKeyValue(int i) { 
            return data[i];
        }

        void clearKeyValue(int i) {
            data[i] = null;
        }
        
        BtreePage clonePage() { 
            return new BtreePageOfString(getStorage());
        }

        int compare(Key key, int i) {
            return ((String)key.oval).compareTo(data[i]);
        }

        void insert(BtreeKey key, int i) { 
            items.setObject(i, key.node);
            data[i] = (String)key.key.oval;
        }

        void memset(int i, int len) { 
            while (--len >= 0) { 
                items.setObject(i, null);
                data[i] = null;
                i += 1;
            }
        }

        boolean prefixSearch(String key, int height, ArrayList result)
        {
            int l = 0, n = nItems, r = n;
            height -= 1;
            while (l < r)  {
                int i = (l+r) >> 1;
                if (!key.startsWith(data[i]) && key.compareTo(data[i]) > 0) {
                    l = i + 1; 
                } else { 
                    r = i;
                }
            }
            Assert.that(r == l); 
            if (height == 0) { 
                while (l < n) { 
                    if (key.compareTo(data[l]) < 0) { 
                        return false;
                    }
                    result.add(items.get(l));
                    l += 1;
                }
            } else { 
                do {
                    if (!((BtreePageOfString)items.get(l)).prefixSearch(key, height, result)) {
                        return false;
                    }
                    if (l == n) { 
                        return true;
                    }
                } while (key.compareTo(data[l++]) >= 0);
                return false;
            }
            return true;
        }    


        BtreePageOfString(Storage s) {
            super(s, MAX_ITEMS);
            data = new String[MAX_ITEMS];
        }

        BtreePageOfString() {}
    }

    static class BtreePageOfValue extends BtreePage { 
        Object[] data; 

        static final int MAX_ITEMS = 100;
            
        Object getData() { 
            return data;
        }

        Key getKey(int i) { 
            return new Key((IValue)data[i]);
        }

        Object getKeyValue(int i) { 
            return data[i];
        }

        void clearKeyValue(int i) {
            data[i] = null;
        }
        
        BtreePage clonePage() { 
            return new BtreePageOfValue(getStorage());
        }

        int compare(Key key, int i) {
            return ((Comparable)key.oval).compareTo(data[i]);
        }

        void insert(BtreeKey key, int i) { 
            items.setObject(i, key.node);
            data[i] = key.key.oval;
        }

        BtreePageOfValue(Storage s) {
            super(s, MAX_ITEMS);
            data = new Object[MAX_ITEMS];
        }

        BtreePageOfValue() {}
    }



    static int checkType(Class c) { 
        int elemType = ClassDescriptor.getTypeCode(c);
        if (elemType > ClassDescriptor.tpObject 
            && elemType != ClassDescriptor.tpValue
            && elemType != ClassDescriptor.tpEnum) 
        { 
            throw new StorageError(StorageError.UNSUPPORTED_INDEX_TYPE, c);
        }
        return elemType;
    }
       
    RndBtree(Class cls, boolean unique) {
        this.unique = unique;
        type = checkType(cls);
    }

    RndBtree(int type, boolean unique) { 
        this.type = type;
        this.unique = unique;
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
        case ClassDescriptor.tpString:
            return String.class;
        case ClassDescriptor.tpDate:
            return Date.class;
        case ClassDescriptor.tpObject:
            return Object.class;
        case ClassDescriptor.tpValue:
            return IValue.class;
        case ClassDescriptor.tpEnum:
            return Enum.class;
       default:
            return null;
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
            if (key.oval instanceof char[]) { 
                key = new Key(new String((char[])key.oval), key.inclusion != 0);
            }
        }
        return key;
    }    

    public T get(Key key) { 
        key = checkKey(key);
        if (root != null) { 
            ArrayList list = new ArrayList();
            root.find(key, key, height, list);
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
        if (root != null) { 
            ((BtreePageOfString)root).prefixSearch(key, height, list);
        }
        return list;
    }

    public Object[] prefixSearch(String key) {
        ArrayList<T> list = prefixSearchList(key);
        return list.toArray();
    }

    public ArrayList<T> getList(Key from, Key till) {
        ArrayList<T> list = new ArrayList<T>();
        if (root != null) { 
            root.find(checkKey(from), checkKey(till), height, list);
        }
        return list;
    }

    public ArrayList<T> getList(Object from, Object till) {
        return getList(Btree.getKeyFromObject(type, from), Btree.getKeyFromObject(type, till));
    }

    public Object[] get(Key from, Key till) {
        ArrayList<T> list = getList(from, till);
        return list.toArray();
    }

    public Object[] get(Object from, Object till) {
        return get(Btree.getKeyFromObject(type, from), Btree.getKeyFromObject(type, till));
    }

    public boolean put(Key key, T obj) {
        return insert(key, obj, false) == null;
    }

    public T set(Key key, T obj) {
        return insert(key, obj, true);
    }

    final void allocateRootPage(BtreeKey ins, int height) { 
        Storage s = getStorage();
        BtreePage newRoot = null;
        switch (type) { 
        case ClassDescriptor.tpByte:
            newRoot = new BtreePageOfByte(s);
            break;
        case ClassDescriptor.tpShort:
            newRoot = new BtreePageOfShort(s);
            break;
        case ClassDescriptor.tpChar:
            newRoot = new BtreePageOfChar(s);
            break;
        case ClassDescriptor.tpBoolean:
            newRoot = new BtreePageOfBoolean(s);
            break;
        case ClassDescriptor.tpInt:
        case ClassDescriptor.tpEnum:
            newRoot = new BtreePageOfInt(s);
            break;
        case ClassDescriptor.tpLong:
            newRoot = new BtreePageOfLong(s);
            break;
        case ClassDescriptor.tpFloat:
            newRoot = new BtreePageOfFloat(s);
            break;
        case ClassDescriptor.tpDouble:
            newRoot = new BtreePageOfDouble(s);
            break;
        case ClassDescriptor.tpObject:
            newRoot = new BtreePageOfObject(s);
            break;
        case ClassDescriptor.tpString:
            newRoot = new BtreePageOfString(s);
            break;
        case ClassDescriptor.tpValue:
            newRoot = new BtreePageOfValue(s);
            break;
        default:
            Assert.failed("Invalid type");
        }
        newRoot.insert(ins, 0, height);
        newRoot.items.setObject(1, root);
        if (height != 0) { 
            newRoot.countChildren(1, height);
        }
        newRoot.nItems = 1;
        root = newRoot;
    }

    final T insert(Key key, T obj, boolean overwrite) {
        BtreeKey ins = new BtreeKey(checkKey(key), obj);
        if (root == null) { 
            allocateRootPage(ins, 0);
            height = 1;
        } else { 
            int result = root.insert(ins, height, unique, overwrite);
            if (result == op_overflow) { 
                allocateRootPage(ins, height);
                height += 1;
            } else if (result == op_duplicate || result == op_overwrite) { 
                return (T)ins.oldNode;
            }
        }
        updateCounter += 1;
        nElems += 1;
        modify();
        return null;
    }

    public void remove(Key key, T obj) 
    {
        remove(new BtreeKey(checkKey(key), obj));
    }
    
    public boolean unlink(Key key, T obj) {
        return removeIfExists(key, obj);
    }
    
    boolean removeIfExists(Key key, Object obj) 
    {
        return removeIfExists(new BtreeKey(checkKey(key), obj));
    }

    void remove(BtreeKey rem) 
    {
        if (!removeIfExists(rem)) { 
            throw new StorageError(StorageError.KEY_NOT_FOUND);
        }
    }

    boolean removeIfExists(BtreeKey rem) 
    {
        if (root == null) {
            return false;
        }
        int result = root.remove(rem, height);
        if (result == op_not_found) { 
            return false;
        }
        nElems -= 1;
        if (result == op_underflow) { 
            if (root.nItems == 0) {                         
                BtreePage newRoot = null;
                if (height != 1) { 
                    newRoot = (BtreePage)root.items.get(0);
                }
                root.deallocate();
                root = newRoot;
                height -= 1;
            }
        }
        updateCounter += 1;
        modify();
        return true;
    }
        
    public T remove(Key key) {
        if (!unique) { 
            throw new StorageError(StorageError.KEY_NOT_UNIQUE);
        }
        BtreeKey rk = new BtreeKey(checkKey(key), null);
        remove(rk);
        return (T)rk.oldNode;
    }
        
        
    public T get(Object key) { 
        return get(Btree.getKeyFromObject(type, key));
    }

    public ArrayList<T> getPrefixList(String prefix) { 
        return getList(new Key(prefix, true), new Key(prefix + Character.MAX_VALUE, false));
    }

    public Object[] getPrefix(String prefix) { 
        return get(new Key(prefix, true), new Key(prefix + Character.MAX_VALUE, false));
    }

    public boolean put(Object key, T obj) {
        return put(Btree.getKeyFromObject(type, key), obj);
    }

    public T set(Object key, T obj) {
        return set(Btree.getKeyFromObject(type, key), obj);
    }

    public void remove(Object key, T obj) {
        remove(Btree.getKeyFromObject(type, key), obj);
    }
    
    public T remove(String key) {
        return remove(new Key(key));
    }

    public T removeKey(Object key) {
        return removeKey(Btree.getKeyFromObject(type, key));
    }

    public int size() {
        return nElems;
    }
    
    public void clear() {
        if (root != null) { 
            root.purge(height);
            root = null;
            nElems = 0;
            height = 0;
            updateCounter += 1;
            modify();
        }
    }
        
    public Object[] toArray() {
        Object[] arr = new Object[nElems];
        if (root != null) { 
            root.traverseForward(height, arr, 0);
        }
        return arr;
    }

    public <E> E[] toArray(E[] arr) {
        if (arr.length < nElems) { 
            arr = (E[])Array.newInstance(arr.getClass().getComponentType(), nElems);
        }
        if (root != null) { 
            root.traverseForward(height, arr, 0);
        }
        if (arr.length > nElems) { 
            arr[nElems] = null;
        }
        return arr;
    }

    public void deallocate() { 
        if (root != null) { 
            root.purge(height);
        }
        super.deallocate();
    }

    static class BtreeEntry<T> implements Map.Entry<Object,T> {
        public Object getKey() {
            return pg.getKeyValue(pos);
        }

        public T getValue() {
            return (T)pg.items.get(pos);
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

        BtreeEntry(BtreePage pg, int pos) {
            this.pg = pg;
            this.pos = pos;
        }

        private BtreePage pg;
        private int pos;
    }


    public Iterator<T> iterator() { 
        return iterator(null, null, ASCENT_ORDER);
    }

    public IterableIterator<Map.Entry<Object,T>> entryIterator() { 
        return entryIterator(null, null, ASCENT_ORDER);
    }

    class BtreeSelectionIterator<E> extends IterableIterator<E> implements PersistentIterator { 
        BtreeSelectionIterator(Key from, Key till, int order) { 
            this.from = from;
            this.till = till;
            this.order = order;
            reset();
        }

        BtreeSelectionIterator(int order) { 
            this.order = order;
        }

        void reset() { 
            int i, l, r;
            
            sp = 0;
            counter = updateCounter;
            if (height == 0) { 
                return;
            }
            BtreePage page = root;
            int h = height;
            
            pageStack = new BtreePage[h];
            posStack =  new int[h];
            
            if (order == ASCENT_ORDER) { 
                if (from == null) { 
                    while (--h > 0) { 
                        posStack[sp] = 0;
                        pageStack[sp] = page;
                        page = (BtreePage)page.items.get(0);
                        sp += 1;
                    }
                    posStack[sp] = 0;
                    pageStack[sp] = page;
                    end = page.nItems;
                    sp += 1;                     
                } else { 
                    while (--h > 0) { 
                        pageStack[sp] = page;
                        l = 0;
                        r = page.nItems;
                        while (l < r)  {
                            i = (l+r) >> 1;
                            if (page.compare(from, i) >= from.inclusion) {
                                l = i + 1; 
                            } else { 
                                r = i;
                            }
                        }
                        Assert.that(r == l); 
                        posStack[sp] = r;
                        page = (BtreePage)page.items.get(r);
                        sp += 1;
                    }
                    pageStack[sp] = page;
                    l = 0;
                    r = end = page.nItems;
                    while (l < r)  {
                        i = (l+r) >> 1;
                        if (page.compare(from, i) >= from.inclusion) {
                            l = i + 1; 
                        } else { 
                            r = i;
                        }
                    }
                    Assert.that(r == l); 
                    if (r == end) {
                        sp += 1;
                        gotoNextItem(page, r-1);
                    } else { 
                        posStack[sp++] = r;
                    }
                }
                if (sp != 0 && till != null) { 
                    page = pageStack[sp-1];
                    if (-page.compare(till, posStack[sp-1]) >= till.inclusion) { 
                        sp = 0;
                    }
                }
            } else { // descent order
                if (till == null) { 
                    while (--h > 0) { 
                        pageStack[sp] = page;
                        posStack[sp] = page.nItems;
                        page = (BtreePage)page.items.get(page.nItems);
                        sp += 1;
                    }
                    pageStack[sp] = page;
                    posStack[sp++] = page.nItems-1;
                } else {
                    while (--h > 0) { 
                        pageStack[sp] = page;
                        l = 0;
                        r = page.nItems;
                        while (l < r)  {
                            i = (l+r) >> 1;
                            if (page.compare(till, i) >= 1-till.inclusion) {
                                l = i + 1; 
                            } else { 
                                r = i;
                            }
                        }
                        Assert.that(r == l); 
                        posStack[sp] = r;
                        page = (BtreePage)page.items.get(r);
                        sp += 1;
                    }
                    pageStack[sp] = page;
                    l = 0;
                    r = page.nItems;
                    while (l < r)  {
                        i = (l+r) >> 1;
                        if (page.compare(till, i) >= 1-till.inclusion) {
                            l = i + 1; 
                        } else { 
                            r = i;
                        }
                    }
                    Assert.that(r == l);  
                    if (r == 0) { 
                        sp += 1;
                        gotoNextItem(page, r);
                    } else { 
                        posStack[sp++] = r-1;
                    }
                }
                if (sp != 0 && from != null) { 
                    page = pageStack[sp-1];
                    if (page.compare(from, posStack[sp-1]) >= from.inclusion) { 
                        sp = 0;
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
            int pos = posStack[sp-1];   
            BtreePage pg = pageStack[sp-1];
            currPos = pos;
            currPage = pg;
            E curr = (E)getCurrent(pg, pos);
            if (((StorageImpl)getStorage()).concurrentIterator) { 
                currKey = new BtreeKey(pg.getKey(pos), pg.items.getRaw(pos));
            }
            gotoNextItem(pg, pos);
            return curr;
        }

        public int nextOid() {
            if (!hasNext()) { 
                return 0;
            }
            int pos = posStack[sp-1];   
            BtreePage pg = pageStack[sp-1];
            currPos = pos;
            currPage = pg;
            Object obj = pg.items.getRaw(pos);
            int oid = getStorage().getOid(obj);
            if (((StorageImpl)getStorage()).concurrentIterator) { 
                currKey = new BtreeKey(pg.getKey(pos), pg.items.getRaw(pos));
            }
            gotoNextItem(pg, pos);
            return oid;
        }

        protected Object getCurrent(BtreePage pg, int pos) { 
            return pg.items.get(pos);
        }

        protected final void gotoNextItem(BtreePage pg, int pos)
        {
            if (order == ASCENT_ORDER) {                     
                if (++pos == end) { 
                    while (--sp != 0) { 
                        pos = posStack[sp-1];
                        pg = pageStack[sp-1];
                        if (++pos <= pg.nItems) {
                            posStack[sp-1] = pos;
                            do { 
                                pg = (BtreePage)pg.items.get(pos);
                                end = pg.nItems;
                                pageStack[sp] = pg;
                                posStack[sp] = pos = 0;
                            } while (++sp < pageStack.length);
                            break;
                        }
                    }
                } else { 
                    posStack[sp-1] = pos;
                }
                if (sp != 0 && till != null && -pg.compare(till, pos) >= till.inclusion) { 
                    sp = 0;
                }
            } else { // descent order
                if (--pos < 0) { 
                    while (--sp != 0) { 
                        pos = posStack[sp-1];
                        pg = pageStack[sp-1];
                        if (--pos >= 0) {
                            posStack[sp-1] = pos;
                            do { 
                                pg = (BtreePage)pg.items.get(pos);
                                pageStack[sp] = pg;
                                posStack[sp] = pos = pg.nItems;
                            } while (++sp < pageStack.length);
                            posStack[sp-1] = --pos;
                            break;
                        }
                    }
                } else { 
                    posStack[sp-1] = pos;
                }
                if (sp != 0 && from != null && pg.compare(from, pos) >= from.inclusion) { 
                    sp = 0;
                }                    
            }
            if (((StorageImpl)getStorage()).concurrentIterator && sp != 0) { 
                nextKey = pg.getKey(pos);
                nextObj = pg.items.getRaw(pos);
            }
        }


        private void refresh() { 
            if (sp != 0) { 
                if (nextKey == null) { 
                    reset();
                } else { 
                    if (order == ASCENT_ORDER) { 
                        from = nextKey;
                    } else { 
                        till = nextKey;
                    }
                    Object next = nextObj;
                    reset();
                    while (true) { 
                        int pos = posStack[sp-1];   
                        BtreePage pg = pageStack[sp-1];
                        if (!pg.items.getRaw(pos).equals(next)) { 
                            gotoNextItem(pg, pos);
                        } else { 
                            break;
                        }
                    }
                }
            }
            counter = updateCounter;
        }

        public void remove() { 
            if (currPage == null) { 
                throw new NoSuchElementException();
            }
            StorageImpl db = (StorageImpl)getStorage();
            if (!db.concurrentIterator) { 
                if (counter != updateCounter) { 
                    throw new ConcurrentModificationException();
                }
                currKey = new BtreeKey(currPage.getKey(currPos), currPage.items.getRaw(currPos));
                if (sp != 0) { 
                    int pos = posStack[sp-1];   
                    BtreePage pg = pageStack[sp-1];
                    nextKey = pg.getKey(pos);
                    nextObj = pg.items.getRaw(pos);
                }
            }
            RndBtree.this.removeIfExists(currKey);
            refresh();
            currPage = null;
        }

        BtreePage[] pageStack;
        int[]       posStack;
        BtreePage   currPage;
        int         currPos;
        int         sp;
        int         end;
        Key         from;
        Key         till;
        int         order;
        int         counter;
        BtreeKey    currKey;
        Key         nextKey;
        Object      nextObj;
    }

    class BtreeSelectionEntryIterator extends BtreeSelectionIterator<Map.Entry<Object,T>> { 
        BtreeSelectionEntryIterator(Key from, Key till, int order) {
            super(from, till, order);
        }
            
        BtreeSelectionEntryIterator(int order) {
            super(order);
        }
            
        protected Object getCurrent(BtreePage pg, int pos) { 
            return new BtreeEntry(pg, pos);
        }
    }

    class BtreeEntryStartFromIterator extends BtreeSelectionEntryIterator
    { 
        BtreeEntryStartFromIterator(int start, int order) {
            super(order);
            this.start = start;
            reset();
        }
        
        void reset() { 
            sp = 0;
            counter = updateCounter;
            if (height == 0 || start >= nElems) {
                return;
            }
            BtreePage page = root;
            int h = height;
            int i = start;
            pageStack = new BtreePage[h];
            posStack = new int[h];
            
            while (--h > 0) {
                pageStack[sp] = page;
                int j;
                for (j = 0; i >= page.nChildren[j]; j++) {
                    i -= page.nChildren[j];
                }
                posStack[sp] = j;
                page = (BtreePage) page.items.get(j);
                sp += 1;
            }
            pageStack[sp] = page;
            posStack[sp++] = i;
            end = page.nItems;
        }
        
        int start;
    }

    public IterableIterator<T> iterator(Key from, Key till, int order) { 
        return new BtreeSelectionIterator<T>(checkKey(from), checkKey(till), order);
    }

    public IterableIterator<T> iterator(Object from, Object till, int order) { 
        return new BtreeSelectionIterator<T>(checkKey(Btree.getKeyFromObject(type, from)), 
                                             checkKey(Btree.getKeyFromObject(type, till)), order);
    }

    public IterableIterator<T> prefixIterator(String prefix) {
        return prefixIterator(prefix, ASCENT_ORDER);
    }

    public IterableIterator<T> prefixIterator(String prefix, int order) {
        return iterator(new Key(prefix), new Key(prefix + Character.MAX_VALUE, false), order);
    }


    public IterableIterator<Map.Entry<Object,T>> entryIterator(Key from, Key till, int order) { 
        return new BtreeSelectionEntryIterator(checkKey(from), checkKey(till), order);
    }

    public IterableIterator<Map.Entry<Object,T>> entryIterator(Object from, Object till, int order) { 
        return new BtreeSelectionEntryIterator(checkKey(Btree.getKeyFromObject(type, from)), 
                                               checkKey(Btree.getKeyFromObject(type, till)), order);
    }

    public T getAt(int i) {
        if (i < 0 || i >= nElems) {
            throw new IndexOutOfBoundsException("Position " + i + ", index size "  + nElems);
        }            
        return (T)root.getAt(i, height);
    }

    public int indexOf(Key key) { 
        return root != null ? root.indexOf(key, height) : -1;
    }


    public IterableIterator<Map.Entry<Object,T>> entryIterator(int start, int order) {
        return new BtreeEntryStartFromIterator(start, order);
    }

    public boolean isUnique() {
        return unique;
    }
}

