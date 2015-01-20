package org.garret.perst.impl;
import  org.garret.perst.*;
import  java.lang.reflect.*;
import  java.util.*;

public final class ClassDescriptor extends Persistent { 
    ClassDescriptor   next;
    String            name;
    boolean           hasReferences;
    FieldDescriptor[] allFields;
    CustomAllocator   allocator;

    static class FieldDescriptor extends Persistent implements Comparable { 
        String          fieldName;
        String          className;
        int             type;
        ClassDescriptor valueDesc;
        transient Field field;

        public int compareTo(Object o) { 
            return fieldName.compareTo(((FieldDescriptor)o).fieldName);
        }

        public boolean equals(FieldDescriptor fd) { 
            return fieldName.equals(fd.fieldName) 
                && className.equals(fd.className)
                && valueDesc == fd.valueDesc
                && type == fd.type;
        }
    }    

    transient Class       cls;
    transient Constructor loadConstructor;
    transient LoadFactory factory;
    transient Object[]    constructorParams;
    transient boolean     customSerializable;
    transient boolean     hasSubclasses;
    transient boolean     resolved;
    transient boolean     isCollection;
    transient boolean     isMap;
    
    static ReflectionProvider reflectionProvider; 

    static boolean            reverseMembersOrder;

    public static final int tpBoolean          = 0;
    public static final int tpByte             = 1;
    public static final int tpChar             = 2;
    public static final int tpShort            = 3;
    public static final int tpInt              = 4;
    public static final int tpLong             = 5;
    public static final int tpFloat            = 6;
    public static final int tpDouble           = 7;
    public static final int tpString           = 8;
    public static final int tpDate             = 9;
    public static final int tpObject           = 10;
    public static final int tpValue            = 11;
    public static final int tpRaw              = 12;
    public static final int tpLink             = 13;
    public static final int tpEnum             = 14;
    public static final int tpCustom           = 15;
    public static final int tpArrayOfBoolean   = 20;
    public static final int tpArrayOfByte      = 21;
    public static final int tpArrayOfChar      = 22;
    public static final int tpArrayOfShort     = 23;
    public static final int tpArrayOfInt       = 24;
    public static final int tpArrayOfLong      = 25;
    public static final int tpArrayOfFloat     = 26;
    public static final int tpArrayOfDouble    = 27;
    public static final int tpArrayOfString    = 28;
    public static final int tpArrayOfDate      = 29;
    public static final int tpArrayOfObject    = 30;
    public static final int tpArrayOfValue     = 31;
    public static final int tpArrayOfRaw       = 32;
    public static final int tpArrayOfLink      = 33; // not supported
    public static final int tpArrayOfEnum      = 34;
    public static final int tpClass            = 35;

    public static final int tpValueTypeBias    = 100;

    static final String signature[] = {
        "boolean", 
        "byte",
        "char",
        "short",
        "int",
        "long",
        "float",
        "double",
        "String",
        "Date",
        "Object",
        "Value",
        "Raw",
        "Link",
        "enum",
        "", 
        "", 
        "", 
        "", 
        "", 
        "", 
        "ArrayOfBoolean",
        "ArrayOfByte",
        "ArrayOfChar",
        "ArrayOfShort",
        "ArrayOfInt",
        "ArrayOfLong",
        "ArrayOfFloat",
        "ArrayOfDouble",
        "ArrayOfEnum",
        "ArrayOfString",
        "ArrayOfDate",
        "ArrayOfObject",
        "ArrayOfValue",
        "ArrayOfRaw",
        "ArrayOfLink",
        "ArrayOfEnum",
        "Class"
    };
        

    static final int sizeof[] = {
        1, // tpBoolean
        1, // tpByte
        2, // tpChar
        2, // tpShort
        4, // tpInt
        8, // tpLong
        4, // tpFloat
        8, // tpDouble
        0, // tpString
        8, // tpDate
        4, // tpObject
        0, // tpValue
        0, // tpRaw
        0, // tpLink
        4  // tpEnum
     };

    static final Class[] perstConstructorProfile = new Class[]{ClassDescriptor.class};

    static ReflectionProvider getReflectionProvider() { 
        if (reflectionProvider == null) { 
            try {
                Class.forName("sun.misc.Unsafe");
                String cls = "org.garret.perst.impl.sun14.Sun14ReflectionProvider";
                reflectionProvider = (ReflectionProvider)Class.forName(cls).newInstance();
            } 
            catch (Throwable x) 
            { 
                reflectionProvider = new StandardReflectionProvider();
            }
        }
        return reflectionProvider;
    } 
           

    public boolean equals(ClassDescriptor cd) { 
        if (cd == null || allFields.length != cd.allFields.length) { 
            return false;
        }
        for (int i = 0; i < allFields.length; i++) { 
            if (!allFields[i].equals(cd.allFields[i])) { 
                return false;
            }
        }
        return true;
    }
        

    Object newInstance() {
        if (factory != null) { 
            return factory.create(this);
        } else { 
            try { 
                return loadConstructor.newInstance(constructorParams);
            } catch (Exception x) { 
                throw new StorageError(StorageError.CONSTRUCTOR_FAILURE, cls, x);
            }
        }
    }

    void buildFieldList(StorageImpl storage, Class cls, ArrayList list) { 
        Class superclass = cls.getSuperclass();
        if (superclass != null) { 
            buildFieldList(storage, superclass, list);
        }
        Field[] flds = cls.getDeclaredFields();
        if (storage.getDatabaseFormatVersion() >= 2) { 
            Arrays.sort(flds, new Comparator<Field>() { public int compare(Field f1, Field f2) { return f1.getName().compareTo(f2.getName()); } });
        } else { // preserve backward compatibility
            if (ClassDescriptor.class.equals(cls)) { 
                for (int i = 0; i < flds.length; i++) { 
                    if ((flds[i].getModifiers() & (Modifier.TRANSIENT|Modifier.STATIC)) == 0) {
                        if (!"next".equals(flds[i].getName())) { 
                            reverseMembersOrder = true;
                        }
                        break;
                    }
                }
            }
            if (reverseMembersOrder) { 
                for (int i = 0, n = flds.length; i < (n >> 1); i++) { 
                    Field f = flds[i];
                    flds[i] = flds[n-i-1];
                    flds[n-i-1] = f;
                }
            }
        }
        for (int i = 0; i < flds.length; i++) { 
            Field f = flds[i];
            if (!f.isSynthetic() && (f.getModifiers() & (Modifier.TRANSIENT|Modifier.STATIC)) == 0) {
                try { 
                    f.setAccessible(true);
                } catch (Exception x) {}
                FieldDescriptor fd = new FieldDescriptor();
                fd.field = f;
                fd.fieldName = f.getName();
                fd.className = cls.getName();
                int type = getTypeCode(f.getType());
                switch (type) {
                  case tpObject:
                  case tpLink:
                  case tpArrayOfObject:
                    hasReferences = true;
                    break;
                  case tpValue:
                    fd.valueDesc = storage.getClassDescriptor(f.getType());
                    hasReferences |= fd.valueDesc.hasReferences;                    
                    break;
                  case tpArrayOfValue:
                    fd.valueDesc = storage.getClassDescriptor(f.getType().getComponentType());
                    hasReferences |= fd.valueDesc.hasReferences;
                }
                fd.type = type;
                list.add(fd);
            }
        }
    }

    public static boolean isEmbedded(Object obj)
    {
        if (obj != null) { 
            Class cls = obj.getClass();
            return obj instanceof IValue || obj instanceof Number || cls.isArray() || cls == Character.class || cls == Boolean.class || cls == Date.class || cls == String.class;
        }
        return false;
    }

    public static int getTypeCode(Class c) { 
        int type;
        if (c.equals(byte.class)) { 
            type = tpByte;
        } else if (c.equals(short.class)) {
            type = tpShort;
        } else if (c.equals(char.class)) {
            type = tpChar;
        } else if (c.equals(int.class)) {
            type = tpInt;
        } else if (c.equals(long.class)) {
            type = tpLong;
        } else if (c.equals(float.class)) {
            type = tpFloat;
        } else if (c.equals(double.class)) {
            type = tpDouble;
        } else if (c.equals(String.class)) {
            type = tpString;
        } else if (c.equals(boolean.class)) {
            type = tpBoolean;
        } else if (c.isEnum()) {
            type = tpEnum;
        } else if (c.equals(java.util.Date.class)) {
            type = tpDate;
        } else if (IValue.class.isAssignableFrom(c)) {
            type = tpValue;
        } else if (c.equals(Link.class)) {
            type = tpLink;
        } else if (c.equals(Class.class)) {
            type = tpClass;
        } else if (c.isArray()) { 
            type = getTypeCode(c.getComponentType());
            if (type >= tpLink && type != tpEnum) { 
                throw new StorageError(StorageError.UNSUPPORTED_TYPE, c);
            }
            type += tpArrayOfBoolean;
        } else if (CustomSerializable.class.isAssignableFrom(c)) {
            type = tpCustom;            
        } else if (IPersistent.class.isAssignableFrom(c)) {
            type = tpObject;
        } else if (serializeNonPersistentObjects) {
            type = tpRaw;            
        } else if (treateAnyNonPersistentObjectAsValue) {
            if (c.equals(Object.class)) { 
                throw new StorageError(StorageError.EMPTY_VALUE);
            }
            type = tpValue;            
        } else {
            type = tpObject;
        }
        return type;
    }

    static boolean treateAnyNonPersistentObjectAsValue = Boolean.getBoolean("perst.implicit.values");
    static boolean serializeNonPersistentObjects = Boolean.getBoolean("perst.serialize.transient.objects");

    ClassDescriptor() {}

    private static Class loadClass(String name) throws ClassNotFoundException { 
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader != null) { 
            try { 
                return loader.loadClass(name);
            } catch (ClassNotFoundException x) {}
        }
        return Class.forName(name);
    }

    private void locateConstructor() { 
        try { 
            Class c = loadClass(cls.getName() + "LoadFactory");
            factory = (LoadFactory)c.newInstance();
        } catch (Exception x1) { 
            try {             
                loadConstructor = cls.getDeclaredConstructor(perstConstructorProfile);
                constructorParams = new Object[]{this};
            } catch (NoSuchMethodException x2) {
                try { 
                    loadConstructor = getReflectionProvider().getDefaultConstructor(cls);
                    constructorParams = null;
                } catch (Exception x3) {
                    throw new StorageError(StorageError.DESCRIPTOR_FAILURE, cls, x3);
                }
            }
            try { 
                loadConstructor.setAccessible(true);
            } catch (Exception x) {}
        }
    }

    ClassDescriptor(StorageImpl storage, Class cls) { 
        this.cls = cls;
        customSerializable = storage.serializer != null && storage.serializer.isApplicable(cls);
        isCollection = Collection.class.isAssignableFrom(cls);
        isMap = Map.class.isAssignableFrom(cls);
        name = getClassName(cls);
        ArrayList list = new ArrayList();
        buildFieldList(storage, cls, list);
        allFields = (FieldDescriptor[])list.toArray(new FieldDescriptor[list.size()]);
        locateConstructor();
        resolved = true;
    }

    public static Field locateField(Class scope, String name) { 
        try { 
            do { 
                try { 
                    Field fld = scope.getDeclaredField(name);                
                    try { 
                        fld.setAccessible(true);
                    } catch (Exception e) {}
                    return fld;
                } catch (NoSuchFieldException x) { 
                    scope = scope.getSuperclass();
                }
            } while (scope != null);
        } catch (Exception x) {
            throw new StorageError(StorageError.ACCESS_VIOLATION, scope.getName() + "." + name, x);
        }
        return null;
    }
        
    public static String getClassName(Class cls) { 
        ClassLoader loader = cls.getClassLoader();
        return (loader instanceof INamedClassLoader) 
            ? ((INamedClassLoader)loader).getName() + ':' + cls.getName()
            : cls.getName();
    }
        
    public static Class loadClass(Storage storage, String name) { 
        if (storage != null) { 
            int col = name.indexOf(':');
            ClassLoader loader;
            if (col >= 0) { 
                loader = storage.findClassLoader(name.substring(0, col));
                if (loader == null) { 
                    // just ignore  this class
                    return null; 
                }
                name = name.substring(col+1);
            } else { 
                loader = storage.getClassLoader();
            }
            if (loader != null) { 
                try { 
                    return loader.loadClass(name);
                } catch (ClassNotFoundException x) {}
            }
        }
        try { 
            return loadClass(name);
        } catch (ClassNotFoundException x) { 
            throw new StorageError(StorageError.CLASS_NOT_FOUND, name, x);
        }
    }

    public void onLoad() {         
        StorageImpl s = (StorageImpl)getStorage();
        cls = loadClass(s, name);
        customSerializable = s.serializer != null && s.serializer.isApplicable(cls);
        isCollection = Collection.class.isAssignableFrom(cls);
        isMap = Map.class.isAssignableFrom(cls);
        Class scope = cls;
        int n = allFields.length;
        for (int i = n; --i >= 0;) { 
            FieldDescriptor fd = allFields[i];
            fd.load();
            if (!fd.className.equals(scope.getName())) {
                for (scope = cls; scope != null; scope = scope.getSuperclass()) { 
                    if (fd.className.equals(scope.getName())) {
                        break;
                    }
                }
            }
            if (scope != null) {
                try { 
                    Field f = scope.getDeclaredField(fd.fieldName);
                    if ((f.getModifiers() & (Modifier.TRANSIENT|Modifier.STATIC)) == 0) {
                        try { 
                            f.setAccessible(true);
                        } catch (Exception e) {}
                        fd.field = f;
                    }
                } catch (NoSuchFieldException x) {}
            } else { 
                scope = cls;
            }
        }
        for (int i = n; --i >= 0;) { 
            FieldDescriptor fd = allFields[i];
            if (fd.field == null) { 
            hierarchyLoop:
                for (scope = cls; scope != null; scope = scope.getSuperclass()) { 
                    try { 
                        Field f = scope.getDeclaredField(fd.fieldName);
                        if ((f.getModifiers() & (Modifier.TRANSIENT|Modifier.STATIC)) == 0) {
                            for (int j = 0; j < n; j++) { 
                                if (allFields[j].field == f) { 
                                    continue hierarchyLoop;
                                }
                            }
                            try { 
                                f.setAccessible(true);
                            } catch (Exception e) {}
                            fd.field = f;
                            break;
                        }
                    } catch (NoSuchFieldException x) {}
                }
            }
        }
        locateConstructor();
        if (s.classDescMap.get(cls) == null) { 
            s.classDescMap.put(cls, this);
        }
    }

       
    void resolve() {
        if (!resolved) { 
            StorageImpl classStorage = (StorageImpl)getStorage();
            ClassDescriptor desc = new ClassDescriptor(classStorage, cls);
            resolved = true;
            if (!desc.equals(this)) { 
                classStorage.registerClassDescriptor(desc);
            }
        }
    }            

    public boolean recursiveLoading() { 
        return false;
    }
}
