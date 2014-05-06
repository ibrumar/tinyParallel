package interp;

public class ParallelException extends Exception {

    public ParallelException() {
        System.err.println("Parallel Exception: Illegal use of parallelism");
    }

    public ParallelException(String message) {
        System.err.println("Parallel Exception: " + message);
    } 

}
