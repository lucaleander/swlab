package org.garret.perst;

/**
 * Exception raised by <code>Assert</code> class when assertion was failed.
 */
public class AssertionFailed extends Error {
    public AssertionFailed() { 
        super("Assertion failed");
    }

    public AssertionFailed(String description) { 
        super("Assertion '" + description + "' failed");
    }
}
