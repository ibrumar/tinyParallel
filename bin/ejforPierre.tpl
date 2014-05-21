int test (){
int i;
return i;
}

int coucou (int blabla, bool doudou, int & eee, int aaa, bool oustiti){
int a;
a = 8;
int b [8];
int c [9];
int ovec[9];

int k;
int o;
o = 92;
k = 7 + 8 + 12;


for (k = 0; k<99; k = k + 7){
int ja;
ja = ja +2;
write ja;
}

begin_parallel
/*first_private_var : k, test; 
private_var : a, o;

{
int ajub;
parallel_for (k = 0; k<99; k = k + 7) reduction(+:o){
    int t;
    int aaaaaa;
    int anonjunaue;*/
first_private_var : k, o; 
private_var : a;
{
parallel_for (k = 0; k<99; k = k + 7){
int t;
t = t+2;
>>>>>>> e0f68b27048b11d89ab30687246f892d870abd26
}

ovec  := c $ 2 $;
c := b $ 4-9+6 $;
} 
end_parallel


}

int papa;
papa = 777;
coucou (papa,false,papa, 77, true);
bool arr [3];
bool arrNew [3];
arr := arrNew $ 2 $;
