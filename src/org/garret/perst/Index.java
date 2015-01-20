package org.garret.perst;

import java.util.*;

/**
 * Interface of object index.
 * Index is used to provide fast access to the object by key. 
 * Object in the index are stored ordered by key value. 
 * It is possible to select object using exact value of the key or 
 * select set of objects which key belongs to the specified interval 
 * (each boundary can be specified or unspecified and can be inclusive or exclusive)
 * Key should be of scalar, String, java.util.Date or peristent object type.
 */
public interface Index<T> extends GenericIndex<T> 
{
    /**
     * Put new object in the index. 
     * @param key object key
     * @param obj object associated with this key. Object can be not yet peristent, in this case
     * its forced to become persistent by assigning OID to it.
     * @return <code>true</code> if object is successfully inserted in the index, 
     * <code>false</code> if index was declared as unique and there is already object with such value
     * of the key in the index. 
     */
    public boolean put(Key key, T obj);

    /**
     * Associate new value with the key. If there is already object with such key in the index, 
     * then it will be removed from the index and new value associated with this key.
     * @param key object key
     * @param obj object associated with this key. Object can be not yet peristent, in this case
     * its forced to become persistent by assigning OID to it.
     * @return object previously associated with this key, <code>null</code> if there was no such object
     */
    public T set(Key key, T obj);

    /**
     * Remove object with specified key from the index
     * @param key value of the key of removed object
     * @param obj object removed from the index
     * @exception StorageError(StorageError.KEY_NOT_FOUND) exception if there is no such key in the index
     */
    public void remove(Key key, T obj);

    /**
     * Remove object with specified key from the index. The only difference with remove is that it doesn't throw an exception
     * when key is not found but return false
     * @param key value of the key of removed object
     * @param obj object removed from the index
     * @return true if object was successfully deleted, false if it is not found in the index
     */
    public boolean unlink(Key key, T obj);

    /**
     * Remove key from the unique index.
     * @param key value of removed key
     * @return removed object
     * @exception StorageError(StorageError.KEY_NOT_FOUND) exception if there is no such key in the index,
     * or StorageError(StorageError.KEY_NOT_UNIQUE) if index is not unique.
     */
    public T remove(Key key);

    /**
     * Put new object in the index. 
     * @param key packed key
     * @param obj object associated with this key. Object can be not yet peristent, in this case
     * its forced to become persistent by assigning OID to it.
     * @return <code>true</code> if object is successfully inserted in the index, 
     * <code>false</code> if index was declared as unique and there is already object with such value
     * of the key in the index. 
     */
    public boolean put(Object key, T obj);

    /**
     * Associate new value with specified key. If there is already object with such key in the index, 
     * then it will be removed from the index and new value associated with this key.
     * @param key packed key
     * @param obj object associated with this key. Object can be not yet peristent, in this case
     * its forced to become persistent by assigning OID to it.
     * @return object previously associated with this key, <code>null</code> if there was no such object
     */
    public T set(Object key, T obj);

    /**
     * Remove object with specified key from the index
     * @param key value of the key of removed object
     * @param obj object removed from the index
     * @exception StorageError(StorageError.KEY_NOT_FOUND) exception if there is no such key in the index
     */
    public void remove(Object key, T obj);

    /**
     * Remove key from the unique string index.
     * @param key packed value of removed key
     * @return removed object
     * @exception StorageError(StorageError.KEY_NOT_FOUND) exception if there is no such key in the index,
     * or StorageError(StorageError.KEY_NOT_UNIQUE) if index is not unique.
     */
    public T remove(String key);

    /**
     * Remove key from the unique index.
     * @param key packed value of removed key
     * @return removed object
     * @exception StorageError(StorageError.KEY_NOT_FOUND) exception if there is no such key in the index,
     * or StorageError(StorageError.KEY_NOT_UNIQUE) if index is not unique.
     */
    public T removeKey(Object key);
}


