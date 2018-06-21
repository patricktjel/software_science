requires y >= 0
x = 0
i = y
while i > 0 //Invariant: i*2+x = y*2 and i >= 0, decreases i
 i--;
 x+=2
assert x = 2 * y