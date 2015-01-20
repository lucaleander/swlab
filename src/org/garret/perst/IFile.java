package org.garret.perst;

/**
 * Interface of file.
 * Programmer can provide its own impleentation of this interface, adding such features
 * as support of flash cards, encrypted files,...
 * Implentation of this interface should throw StorageError exception in case of failure
 */
public interface IFile { 
    /**
     * Write data to the file
     * @param pos offset in the file
     * @param buf array with data to be writter (size is always equal to database page size)
     */
    void write(long pos, byte[] buf);

    /**
     * Read data from the file
     * @param pos offset in the file
     * @param buf array to receive readen data (size is always equal to database page size)
     * @return number of bytes actually readen
     */
    int read(long pos, byte[] buf);

    /**
     * Flush all fiels changes to the disk
     */
    void sync();
        
    /**
     * Try lock file
     * @param shared if lock is shared
     * @return <code>true</code> if file was successfully locked or locking in not implemented,
     * <code>false</code> if file is locked by some other applciation     
     */
    boolean tryLock(boolean shared);

    /**
     * Lock file
     * @param shared if lock is shared
     */
    void lock(boolean shared);

    /**
     * Unlock file
     */
    void unlock();

    /**
     * Close file
     */
    void close();

    /**
     * Length of the file
     */
    long length();
}
