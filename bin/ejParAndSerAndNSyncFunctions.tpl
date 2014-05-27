//test
int test (int & hola){
    int i;
    read i;
    i = 2;
    hola = 3;
    not_sync {
        hola = 3;
    }
    //for another test just comment the following begin_parallel..end_parallel
//    begin_parallel
 //   {
        i = i + 1;
//    } end_parallel
    return i;
}

int hola;
begin_parallel{
//    not_sync {
        test(hola);
//    }
}end_parallel

