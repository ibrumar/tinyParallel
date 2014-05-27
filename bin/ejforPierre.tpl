//test
int coucou (int blabla, bool doudou, int & eee, int aaa, bool oustiti){
int b [8];
int c [9];
int ovec[9];
int k;
int o;
o = 92;
k = 7 + 8 + 12;
int a;

for (k = 0; k<99; k = k + 7){
int ja;
ja = ja +2;
write ja;
}

begin_parallel
first_private_var : k; 
private_var : a;
{
int ajub;
parallel_for (k = 0; k<99; k = k + 7) reduction(+:o){
   o = o+1;
}
       
//first_private_var : k, o; 
//private_var : a;
//{
parallel_for (k = 0; k<99; k = k + 7){
o = o+2;
}

ovec  := c $ 2 $;
c := b $ 4-9+6 $;
} 
end_parallel

}

bool arr [3];
bool arrNew [3];
arr := arrNew $ 2 $;
