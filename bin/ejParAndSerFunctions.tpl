int test (int & hola){
int i;
read i;
i = 2;
hola = 3;
return i;
}

int hola;
test(hola);
