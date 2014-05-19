int test (int & hola, int hola2){
    int i;
    read i;
    i = 2;
    hola = 3;
    not_sync {
        hola = 3;
    }
    //for another test just comment the following begin_parallel..end_parallel
    begin_parallel
    {
        i = getNumThreads();
        write "numero de threads ";
        
        write i;
        write "\n";
        write "thread ID parallel bloc ";
        i = getThreadId();
        write i;
        write "\n";
        i = i + 1;
    } end_parallel
    return i;
}

int hola;
int hola2;
int hola3[10];
hola3[1] = 1;
hola2 = getThreadId();
write "thread ID main ";
write hola2;
test(hola, hola2);
