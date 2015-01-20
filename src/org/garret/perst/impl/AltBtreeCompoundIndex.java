package org.garret.perst.impl;
import  org.garret.perst.*;

import java.util.*;
import java.util.ArrayList;

class AltBtreeCompoundIndex<T> extends AltBtree<T> implements Index<T> { 
    int[]    types;

    AltBtreeCompoundIndex() {}
    
    AltBtreeCompoundIndex(Class[] keyTypes, boolean unique) {
        this.unique = unique;
        type = ClassDescriptor.tpValue;        
        types = new int[keyTypes.length];
        for (int i = 0; i < keyTypes.length; i++) {
            types[i] = getCompoundKeyComponentType(keyTypes[i]);
        }
    }

    static int getCompoundKeyComponentType(Class c) { 
        if (c.equals(Boolean.class)) { 
            return ClassDescriptor.tpBoolean;
        } else if (c.equals(Byte.class)) { 
            return ClassDescriptor.tpByte;
        } else if (c.equals(Character.class)) { 
            return ClassDescriptor.tpChar;
        } else if (c.equals(Short.class)) { 
            return ClassDescriptor.tpShort;
        } else if (c.equals(Integer.class)) { 
            return ClassDescriptor.tpInt;
        } else if (c.equals(Long.class)) { 
            return ClassDescriptor.tpLong;
        } else if (c.equals(Float.class)) { 
            return ClassDescriptor.tpFloat;
        } else if (c.equals(Double.class)) { 
            return ClassDescriptor.tpDouble;
        } else if (c.equals(String.class)) { 
            return ClassDescriptor.tpString;
        } else if (c.equals(Date.class)) { 
            return ClassDescriptor.tpDate;
        } else {
            return ClassDescriptor.tpObject;
        }
    }

    public Class[] getKeyTypes() {
        Class[] keyTypes = new Class[types.length];
        for (int i = 0; i < keyTypes.length; i++) { 
            keyTypes[i] = mapKeyType(types[i]);
        }
        return keyTypes;
    }

    static class CompoundKey implements Comparable, IValue {
        Object[] keys;

        public int compareTo(Object o) { 
            CompoundKey c = (CompoundKey)o;
            int n = keys.length < c.keys.length ? keys.length : c.keys.length; 
            for (int i = 0; i < n; i++) { 
                int diff = ((Comparable)keys[i]).compareTo(c.keys[i]);
                if (diff != 0) { 
                    return diff;
                }
            }
            return 0;  // allow to compare part of the compound key
        }

        CompoundKey(Object[] keys) { 
            this.keys = keys;
        }
    }
                
    private Key convertKey(Key key) { 
        return convertKey(key, true);
    }

    private Key convertKey(Key key, boolean prefix) { 
        if (key == null) { 
            return null;
        }
        if (key.type != ClassDescriptor.tpArrayOfObject) { 
            throw new StorageError(StorageError.INCOMPATIBLE_KEY_TYPE);
        }
        Object[] keyComponents = (Object[])key.oval;
        if ((!prefix && keyComponents.length != types.length) || keyComponents.length > types.length) { 
            throw new StorageError(StorageError.INCOMPATIBLE_KEY_TYPE);
        }
        boolean isCopy = false;
        for (int i = 0; i < keyComponents.length; i++) { 
            int type = types[i];
            if (type == ClassDescriptor.tpObject || type == ClassDescriptor.tpBoolean) { 
                if (!isCopy) { 
                    Object[] newKeyComponents = new Object[keyComponents.length];
                    System.arraycopy(keyComponents, 0, newKeyComponents, 0, keyComponents.length);
                    keyComponents = newKeyComponents;
                    isCopy = true;
                }
                keyComponents[i] = (type == ClassDescriptor.tpObject)
                    ? (Object)new Integer(keyComponents[i] == null ? 0 : getStorage().getOid(keyComponents[i]))
                    : (Object)new Byte((byte)(((Boolean)keyComponents[i]).booleanValue() ? 1 : 0));
                
            }
        }
        return new Key(new CompoundKey(keyComponents), key.inclusion != 0);
    }
            
    public ArrayList<T> getList(Key from, Key till) {
        return super.getList(convertKey(from), convertKey(till));
    }

    public T get(Key key) {
        return super.get(convertKey(key));
    }

    public T  remove(Key key) { 
        return super.remove(convertKey(key, false));
    }

    public void remove(Key key, T obj) { 
        super.remove(convertKey(key, false), obj);
    }

    public T  set(Key key, T obj) { 
        return super.set(convertKey(key, false), obj);
    }

    public boolean put(Key key, T obj) {
        return super.put(convertKey(key, false), obj);
    }

    public IterableIterator<T> iterator(Key from, Key till, int order) {
        return super.iterator(convertKey(from), convertKey(till), order);
    }

    public IterableIterator<Map.Entry<Object,T>> entryIterator(Key from, Key till, int order) {
        return super.entryIterator(convertKey(from), convertKey(till), order);
    }
}

