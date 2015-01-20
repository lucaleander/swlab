package org.garret.perst;

import java.util.*;

/**
 * Collection of version of versioned object.
 * Versioned object should be access through version history object.
 * Instead of storing direct reference to Verson in some component of some other persistent object, 
 * it is necessary to store reference to it's VersionHistory.
 */
public class VersionHistory<V extends Version>  extends PersistentResource 
{
    /**
     * Get current version in version history.
     * Current version can be explicitely set by setVersion or result of last checkOut
     * is used as current version
     */
    public synchronized V getCurrent() { 
        return current;
    }

    /**
     * Set new current version in version history
     * @param version new current version in version history (it must belong to version history)
     */
    public synchronized void setCurrent(V version) { 
        current = version;
        modify();
    }

    /**
     * Checkout current version: create successor of the current version.
     * This version has to be checked-in in order to be placed in version history
     * @return checked-out version
     */
    public synchronized V checkout() {
        Assert.that(current.isCheckedIn());
        return (V)current.newVersion();        
    }

    /**
     * Get root version
     * @return root version in this version history
     */
    public synchronized V getRoot() { 
        return versions.get(0);
    }

    /**
     * Get latest version before specified date
     * @param timestamp deadline, if <code>null</code> then the latest version in version history will be returned
     * @return version with the largest timestamp less than or equal to specified <code>timestamp</code>
     */
    public synchronized V getLatestBefore(Date timestamp) { 
        if (timestamp == null) { 
            return versions.get(versions.size()-1);
        }
        int l = 0, n = versions.size(), r = n;
        long t = timestamp.getTime()+1;
        while (l < r) { 
            int m = (l + r) >> 1;
            if (versions.get(m).getDate().getTime() < t) { 
                l = m + 1;
            } else { 
                r = m;
            }
        }
        return r > 0 ? versions.get(r-1) : null;
    }

    /**
     * Get earliest version after specified date
     * @param timestamp deadline, if <code>null</code> then root version will be returned
     * @return version with the smallest timestamp greater than or equal to specified <code>timestamp</code>
     */
    public synchronized V getEarliestAfter(Date timestamp) { 
        if (timestamp == null) { 
            return versions.get(0);
        }
        int l = 0, n = versions.size(), r = n;
        long t = timestamp.getTime();
        while (l < r) { 
            int m = (l + r) >> 1;
            if (versions.get(m).getDate().getTime() < t) { 
                l = m + 1;
            } else { 
                r = m;
            }
        }
        return r < n ? versions.get(r) : null;
    }


    /**
     * Get version with specified label. If there are more than one version marked with 
     * this label, then the latest one will be returned
     * @param label version label
     * @return latest version with specified label
     */
    public synchronized V getVersionByLabel(String label) { 
        for (int i = versions.size(); --i >= 0;) { 
            V v = versions.get(i);
            if (v.hasLabel(label)) { 
                return v;
            }
        }
        return null;
    }

    /**
     * Get version with specified ID.
     * @param id version ID
     * @return version with specified ID
     */
    public synchronized V getVersionById(String id) { 
        for (int i = versions.size(); --i >= 0;) { 
            V v = versions.get(i);
            if (v.getId().equals(id)) { 
                return v;
            }
        }
        return null;
    }

    /**
     * Get all versions in version history
     * @return array of versions sorted by date
     */
    public synchronized Version[] getAllVersions() {
        return versions.toArray(new Version[versions.size()]);
    }

    /**
     * Get iterator through all version in version history
     * Iteration is started from the root version and performed in direction of increaing
     * version timestamp
     * This iterator supports remove() method.
     * @return version iterator
     */
    public synchronized Iterator<V> iterator() { 
        return versions.iterator();
    }

    /**
     * Create new version history
     * @param root root version
     */
    public VersionHistory(V root) { 
        versions = root.getStorage().<V>createLink(1);
        versions.add(root);
        current = root;
        current.history = this;
    }

    Link<V> versions;
    V       current;
}