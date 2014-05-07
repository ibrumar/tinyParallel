package interp;

public class ParallelException extends Exception {
    public String errorName;
    public ParallelException() {
        errorName = "Parallel Exception: Illegal use of parallelism";
    }

    public ParallelException(String message) {
        errorName = "Parallel Exception: " + message;
    } 

}
