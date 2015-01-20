package org.garret.perst.impl;
import  org.garret.perst.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.ArrayList;

class MultiFieldValue implements Comparable<MultiFieldValue> { 
    Comparable[] values;
    Object       obj;
    
    public int compareTo(MultiFieldValue f) { 
        for (int i = 0; i < values.length; i++) {
            int diff = values[i].compareTo(f.values[i]);
            if (diff != 0) { 
                return diff;
            }
        }
        return 0;
    }
    
    MultiFieldValue(Object obj, Comparable[] values) { 
        this.obj = obj;
        this.values = values;
    }
}
 
class BtreeMultiFieldIndex<T> extends Btree<T> implements FieldIndex<T> { 
    String   className;
    String[] fieldName;
    int[]    types;

    transient Class   cls;
    transient Field[] fld;

    BtreeMultiFieldIndex() {}
    
    BtreeMultiFieldIndex(Class cls, String[] fieldName, boolean unique) {
        this.cls = cls;
        this.unique = unique;
        this.fieldName = fieldName;        
        this.className = ClassDescriptor.getClassName(cls);
        locateFields();
        type = ClassDescriptor.tpArrayOfByte;        
        types = new int[fieldName.length];
        for (int i = 0; i < types.length; i++) {
            types[i] = checkType(fld[i].getType());
        }
    }

    private final void locateFields() 
    {
        fld = new Field[fieldName.length];
        for (int i = 0; i < fieldName.length; i++) {
            fld[i] = ClassDescriptor.locateField(cls, fieldName[i]);
            if (fld[i] == null) { 
                throw new StorageError(StorageError.INDEXED_FIELD_NOT_FOUND, className + "." + fieldName[i]);
            }
        }
    }

    public Class getIndexedClass() { 
        return cls;
    }

    public Field[] getKeyFields() { 
        return fld;
    }

    public void onLoad()
    {
        cls = ClassDescriptor.loadClass(getStorage(), className);
        locateFields();
    }

    int compareByteArrays(byte[] key, byte[] item, int offs, int lengtn) { 
        int o1 = 0;
        int o2 = offs;
        byte[] a1 = key;
        byte[] a2 = item;
        for (int i = 0; i < fld.length && o1 < key.length; i++) {
            int diff = 0;
            switch (types[i]) { 
              case ClassDescriptor.tpBoolean:
              case ClassDescriptor.tpByte:
                diff = a1[o1++] - a2[o2++];
                break;
              case ClassDescriptor.tpShort:
                diff = Bytes.unpack2(a1, o1) - Bytes.unpack2(a2, o2);
                o1 += 2;
                o2 += 2;
                break;
              case ClassDescriptor.tpChar:
                diff = (char)Bytes.unpack2(a1, o1) - (char)Bytes.unpack2(a2, o2);
                o1 += 2;
                o2 += 2;
                break;
              case ClassDescriptor.tpInt:
              case ClassDescriptor.tpObject:
              case ClassDescriptor.tpEnum:
              {
                  int i1 = Bytes.unpack4(a1, o1);
                  int i2 = Bytes.unpack4(a2, o2);
                  diff = i1 < i2 ? -1 : i1 == i2 ? 0 : 1;
                  o1 += 4;
                  o2 += 4;
                  break;
              }
              case ClassDescriptor.tpLong:
              case ClassDescriptor.tpDate:
              {
                  long l1 = Bytes.unpack8(a1, o1);
                  long l2 = Bytes.unpack8(a2, o2);
                  diff = l1 < l2 ? -1 : l1 == l2 ? 0 : 1;
                  o1 += 8;
                  o2 += 8;
                  break;
              }
              case ClassDescriptor.tpFloat:
              {
                  float f1 = Float.intBitsToFloat(Bytes.unpack4(a1, o1));
                  float f2 = Float.intBitsToFloat(Bytes.unpack4(a2, o2));
                  diff = f1 < f2 ? -1 : f1 == f2 ? 0 : 1;
                  o1 += 4;
                  o2 += 4;
                  break;
              }
              case ClassDescriptor.tpDouble:
              {
                  double d1 = Double.longBitsToDouble(Bytes.unpack8(a1, o1));
                  double d2 = Double.longBitsToDouble(Bytes.unpack8(a2, o2));
                  diff = d1 < d2 ? -1 : d1 == d2 ? 0 : 1;
                  o1 += 8;
                  o2 += 8;
                  break;
              }
              case ClassDescriptor.tpString:
              {
                  int len1 = Bytes.unpack4(a1, o1);
                  int len2 = Bytes.unpack4(a2, o2);
                  o1 += 4;
                  o2 += 4;
                  int len = len1 < len2 ? len1 : len2;
                  while (--len >= 0) { 
                      diff = (char)Bytes.unpack2(a1, o1) - (char)Bytes.unpack2(a2, o2);
                      if (diff != 0) { 
                          return diff;
                      }
                      o1 += 2;
                      o2 += 2;
                  }
                  diff = len1 - len2;
                  break;
              }
              case ClassDescriptor.tpArrayOfByte:
              {
                  int len1 = Bytes.unpack4(a1, o1);
                  int len2 = Bytes.unpack4(a2, o2);
                  o1 += 4;
                  o2 += 4;
                  int len = len1 < len2 ? len1 : len2;
                  while (--len >= 0) { 
                      diff = a1[o1++] - a2[o2++];
                      if (diff != 0) { 
                          return diff;
                      }
                  }
                  diff = len1 - len2;
                  break;
              }
              default:
                Assert.failed("Invalid type");
            }
            if (diff != 0) { 
                return diff;
            }
        }
        return 0;
    }

    String convertString(Object s) { 
        return (String)s;
    }

    Object unpackByteArrayKey(Page pg, int pos) {
        int offs = BtreePage.firstKeyOffs + BtreePage.getKeyStrOffs(pg, pos);
        byte[] data = pg.data;
        Object values[] = new Object[fld.length];

        for (int i = 0; i < fld.length; i++) {
            Object v = null;
            switch (types[i]) { 
              case ClassDescriptor.tpBoolean:
                v = Boolean.valueOf(data[offs++] != 0);
                break;
              case ClassDescriptor.tpByte:
                v = new Byte(data[offs++]);
                break;
              case ClassDescriptor.tpShort:
                v = new Short(Bytes.unpack2(data, offs));
                offs += 2;
                break;
              case ClassDescriptor.tpChar:
                v = new Character((char)Bytes.unpack2(data, offs));
                offs += 2;
                break;
              case ClassDescriptor.tpInt:
                v = new Integer(Bytes.unpack4(data, offs));
                offs += 4;
                break;
              case ClassDescriptor.tpObject:
              {
                  int oid = Bytes.unpack4(data, offs);
                  v = oid == 0 ? null : ((StorageImpl)getStorage()).lookupObject(oid, null);
                  offs += 4;
                  break;
              }
              case ClassDescriptor.tpLong:
                v = new Long(Bytes.unpack8(data, offs));
                offs += 8;
                break;
              case ClassDescriptor.tpEnum:
                v = fld[i].getType().getEnumConstants()[Bytes.unpack4(data, offs)];
                offs += 4;
                break;
              case ClassDescriptor.tpDate:
              {
                  long msec = Bytes.unpack8(data, offs);
                  v = msec == -1 ? null : new Date(msec);
                  offs += 8;
                  break;
              }
              case ClassDescriptor.tpFloat:
                v = new Float(Float.intBitsToFloat(Bytes.unpack4(data, offs)));
                offs += 4;
                break;
              case ClassDescriptor.tpDouble:
                v = new Double(Double.longBitsToDouble(Bytes.unpack8(data, offs)));
                offs += 8;
                break;
              case ClassDescriptor.tpString:
              {
                  int len = Bytes.unpack4(data, offs);
                  offs += 4;
                  char[] sval = new char[len];
                  for (int j = 0; j < len; j++) { 
                      sval[j] = (char)Bytes.unpack2(data, offs);
                      offs += 2;
                  }
                  v = new String(sval);
                  break;
              }
              case ClassDescriptor.tpArrayOfByte:
              {
                  int len = Bytes.unpack4(data, offs);
                  offs += 4;
                  byte[] bval = new byte[len];
                  System.arraycopy(data, offs, bval, 0, len);
                  offs += len;
                  break;
              }
              default:
                Assert.failed("Invalid type");
            }
            values[i] = v;
        }
        return values;
    }


    private Key extractKey(Object obj) { 
        try { 
            ByteBuffer buf = new ByteBuffer();
            int dst = 0;
            for (int i = 0; i < fld.length; i++) { 
                Field f = (Field)fld[i];
                switch (types[i]) {
                  case ClassDescriptor.tpBoolean:
                    buf.extend(dst+1);
                    buf.arr[dst++] = (byte)(f.getBoolean(obj) ? 1 : 0);
                    break;
                  case ClassDescriptor.tpByte:
                    buf.extend(dst+1);
                    buf.arr[dst++] = f.getByte(obj);
                    break;
                  case ClassDescriptor.tpShort:
                    buf.extend(dst+2);
                    Bytes.pack2(buf.arr, dst, f.getShort(obj));
                    dst += 2;
                    break;
                  case ClassDescriptor.tpChar:
                    buf.extend(dst+2);
                    Bytes.pack2(buf.arr, dst, (short)f.getChar(obj));
                    dst += 2;
                    break;
                  case ClassDescriptor.tpInt:
                    buf.extend(dst+4);
                    Bytes.pack4(buf.arr, dst, f.getInt(obj));
                    dst += 4;
                    break;
                  case ClassDescriptor.tpObject:
                  {
                      Object p = f.get(obj);
                      buf.extend(dst+4);
                      Bytes.pack4(buf.arr, dst, getStorage().makePersistent(p));
                      dst += 4;
                      break;
                  }
                  case ClassDescriptor.tpLong:
                    buf.extend(dst+8);
                    Bytes.pack8(buf.arr, dst, f.getLong(obj));
                    dst += 8;
                    break;
                  case ClassDescriptor.tpDate:
                  {
                      Date d = (Date)f.get(obj);
                      buf.extend(dst+8);
                      Bytes.pack8(buf.arr, dst, d == null ? -1 : d.getTime());
                      dst += 8;
                      break;
                  }
                  case ClassDescriptor.tpFloat:
                    buf.extend(dst+4);
                    Bytes.pack4(buf.arr, dst, Float.floatToIntBits(f.getFloat(obj)));
                    dst += 4;
                    break;
                  case ClassDescriptor.tpDouble:
                    buf.extend(dst+8);
                    Bytes.pack8(buf.arr, dst, Double.doubleToLongBits(f.getDouble(obj)));
                    dst += 8;
                    break;
                  case ClassDescriptor.tpEnum:
                    buf.extend(dst+4);
                    Bytes.pack4(buf.arr, dst, ((Enum)f.get(obj)).ordinal());
                    dst += 4;
                    break;
                  case ClassDescriptor.tpString:
                  {
                      buf.extend(dst+4);
                      String str = convertString(f.get(obj));
                      if (str != null) { 
                          int len = str.length();
                          Bytes.pack4(buf.arr, dst, len);
                          dst += 4;
                          buf.extend(dst + len*2);
                          for (int j = 0; j < len; j++) { 
                              Bytes.pack2(buf.arr, dst, (short)str.charAt(j));
                              dst += 2;
                          }
                      } else { 
                          Bytes.pack4(buf.arr, dst, 0);
                          dst += 4;
                      }
                      break;
                  }
                  case ClassDescriptor.tpArrayOfByte:
                  {
                      buf.extend(dst+4);
                      byte[] arr = (byte[])f.get(obj);
                      if (arr != null) { 
                          int len = arr.length;
                          Bytes.pack4(buf.arr, dst, len);
                          dst += 4;                          
                          buf.extend(dst + len);
                          System.arraycopy(arr, 0, buf.arr, dst, len);
                          dst += len;
                      } else { 
                          Bytes.pack4(buf.arr, dst, 0);
                          dst += 4;
                      }
                      break;
                  }
                  default:
                    Assert.failed("Invalid type");
                }
            }
            return new Key(buf.toArray());
        } catch (Exception x) { 
            throw new StorageError(StorageError.ACCESS_VIOLATION, x);
        }
    }
            

    private Key convertKey(Key key) { 
        if (key == null) { 
            return null;
        }
        if (key.type != ClassDescriptor.tpArrayOfObject) { 
            throw new StorageError(StorageError.INCOMPATIBLE_KEY_TYPE);
        }
        Object[] values = (Object[])key.oval;
        ByteBuffer buf = new ByteBuffer();
        int dst = 0;
        for (int i = 0; i < values.length; i++) { 
            Object v = values[i];
            switch (types[i]) {
              case ClassDescriptor.tpBoolean:
                buf.extend(dst+1);
                buf.arr[dst++] = (byte)(((Boolean)v).booleanValue() ? 1 : 0);
                break;
              case ClassDescriptor.tpByte:
                buf.extend(dst+1);
                buf.arr[dst++] = ((Number)v).byteValue();
                break;
              case ClassDescriptor.tpShort:
                buf.extend(dst+2);
                Bytes.pack2(buf.arr, dst, ((Number)v).shortValue());
                dst += 2;
                break;
              case ClassDescriptor.tpChar:
                buf.extend(dst+2);
                Bytes.pack2(buf.arr, dst, (v instanceof Number) ? ((Number)v).shortValue() : (short)((Character)v).charValue());
                dst += 2;
                break;
              case ClassDescriptor.tpInt:
                buf.extend(dst+4);
                Bytes.pack4(buf.arr, dst, ((Number)v).intValue());
                dst += 4;
                break;
              case ClassDescriptor.tpObject:
                buf.extend(dst+4);
                Bytes.pack4(buf.arr, dst, getStorage().getOid(v));
                dst += 4;
                break;
              case ClassDescriptor.tpLong:
                buf.extend(dst+8);
                Bytes.pack8(buf.arr, dst, ((Number)v).longValue());
                dst += 8;
                break;
              case ClassDescriptor.tpDate:
                buf.extend(dst+8);
                Bytes.pack8(buf.arr, dst, v == null ? -1 : ((Date)v).getTime());
                dst += 8;
                break;
              case ClassDescriptor.tpFloat:
                buf.extend(dst+4);
                Bytes.pack4(buf.arr, dst, Float.floatToIntBits(((Number)v).floatValue()));
                dst += 4;
                break;
              case ClassDescriptor.tpDouble:
                buf.extend(dst+8);
                Bytes.pack8(buf.arr, dst, Double.doubleToLongBits(((Number)v).doubleValue()));
                dst += 8;
                break;
              case ClassDescriptor.tpEnum:
                buf.extend(dst+4);
                Bytes.pack4(buf.arr, dst, ((Enum)v).ordinal());
                dst += 4;
                break;
              case ClassDescriptor.tpString:
              {
                  buf.extend(dst+4);
                  if (v != null) { 
                      String str = convertString(v);
                      int len = str.length();
                      Bytes.pack4(buf.arr, dst, len);
                      dst += 4;
                      buf.extend(dst + len*2);
                      for (int j = 0; j < len; j++) { 
                          Bytes.pack2(buf.arr, dst, (short)str.charAt(j));
                          dst += 2;
                      }
                  } else { 
                      Bytes.pack4(buf.arr, dst, 0);
                      dst += 4;
                  }
                  break;
              }
              case ClassDescriptor.tpArrayOfByte:
              {
                  buf.extend(dst+4);
                  if (v != null) { 
                      byte[] arr = (byte[])v;
                      int len = arr.length;
                      Bytes.pack4(buf.arr, dst, len);
                      dst += 4;                          
                      buf.extend(dst + len);
                      System.arraycopy(arr, 0, buf.arr, dst, len);
                      dst += len;
                  } else { 
                      Bytes.pack4(buf.arr, dst, 0);
                      dst += 4;
                  }
                  break;
              }
              default:
                Assert.failed("Invalid type");
            }
        }
        return new Key(buf.toArray(), key.inclusion != 0);
    }
            

    public boolean put(T obj) {
        return super.put(extractKey(obj), obj);
    }

    public T set(T obj) {
         return super.set(extractKey(obj), obj);
    }

    public boolean addAll(Collection<? extends T> c) {
        MultiFieldValue[] arr = new MultiFieldValue[c.size()];
	Iterator<? extends T> e = c.iterator();
        try { 
            for (int i = 0; e.hasNext(); i++) {
                T obj = e.next();
                Comparable[] values = new Comparable[fld.length];
                for (int j = 0; j < values.length; j++) { 
                    values[j] = (Comparable)fld[j].get(obj);
                }
                arr[i] = new MultiFieldValue(obj, values);
            }
        } catch (Exception x) { 
            throw new StorageError(StorageError.ACCESS_VIOLATION, x);
        }
        Arrays.sort(arr);
	for (int i = 0; i < arr.length; i++) {
            add((T)arr[i].obj);
        }
	return arr.length > 0;
    }

    public boolean remove(Object obj) {
        return super.removeIfExists(extractKey(obj), obj);
    }

    public T remove(Key key) {
        return super.remove(convertKey(key));
    }
    
    public boolean containsObject(T obj) {
        Key key = extractKey(obj);
        if (unique) { 
            return super.get(key) != null;
        } else { 
            Object[] mbrs = get(key, key);
            for (int i = 0; i < mbrs.length; i++) { 
                if (mbrs[i] == obj) { 
                    return true;
                }
            }
            return false;
        }
    }

    public boolean contains(Object obj) {
        Key key = extractKey(obj);
        if (unique) { 
            return super.get(key) != null;
        } else { 
            Object[] mbrs = get(key, key);
            for (int i = 0; i < mbrs.length; i++) { 
                if (mbrs[i].equals(obj)) { 
                    return true;
                }
            }
            return false;
        }
    }

    public void append(T obj) {
        throw new StorageError(StorageError.UNSUPPORTED_INDEX_TYPE);
    }

    public T[] get(Key from, Key till) {
        ArrayList list = new ArrayList();
        if (root != 0) { 
            BtreePage.find((StorageImpl)getStorage(), root, convertKey(from), convertKey(till), this, height, list);
        }
        return (T[])list.toArray((T[])Array.newInstance(cls, list.size()));
    }

    public T[] getPrefix(String prefix) {
        throw new StorageError(StorageError.INCOMPATIBLE_KEY_TYPE);
    }
        

    public T[] prefixSearch(String key) {
        throw new StorageError(StorageError.INCOMPATIBLE_KEY_TYPE);
    }
        

    public T[] toArray() {
        T[] arr = (T[])Array.newInstance(cls, nElems);
        if (root != 0) { 
            BtreePage.traverseForward((StorageImpl)getStorage(), root, type, height, arr, 0);
        }
        return arr;
    }

    public T get(Key key) {
        return super.get(convertKey(key));
    }

    public IterableIterator<T> iterator(Key from, Key till, int order) {
        return super.iterator(convertKey(from), convertKey(till), order);
    }

    public IterableIterator<Map.Entry<Object,T>> entryIterator(Key from, Key till, int order) {
        return super.entryIterator(convertKey(from), convertKey(till), order);
    }

    public IterableIterator<T> queryByExample(T obj) {
        Key key = extractKey(obj);
        return iterator(key, key, ASCENT_ORDER);
    }
            
    public IterableIterator<T> select(String predicate) { 
        Query<T> query = new QueryImpl<T>(getStorage());
        return query.select(cls, iterator(), predicate);
    }

    public boolean isCaseInsensitive() { 
        return false;
    }
}


class BtreeCaseInsensitiveMultiFieldIndex<T> extends BtreeMultiFieldIndex<T> {    
    BtreeCaseInsensitiveMultiFieldIndex() {}

    BtreeCaseInsensitiveMultiFieldIndex(Class cls, String[] fieldNames, boolean unique) {
        super(cls, fieldNames, unique);
    }

    String convertString(Object s) { 
        return ((String)s).toLowerCase();
    }

    public boolean isCaseInsensitive() { 
        return true;
    }
}
