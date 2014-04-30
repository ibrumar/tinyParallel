package interp;

public class ParallelException extends Exception {

public ParallelException(){
    System.out.println("Illegal use of parallelism out of a parallel zone");
  } 

}
