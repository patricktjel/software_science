// This method adds two numbers.
// If this number is even than we multiply the number by 3
// Else the number is multiplied by 2
// The result will always be even.
method Mul_to_even(n1: int, n2: int) returns (n3:int) {
    n3 := n1 + n2;
    if (n3 % 2 == 0) {
      n3 := n3 * 3;
    } else {
      n3 := n3 * 2;
    }
    assert n3 % 2 == 0;
}

; static single assignment code
n_3 = c_0 ? n_1 + n_2 : n_0
c_1 = c_0 && (n_3 % 2 == 0)
	n_4 = c_1 ? n_3 * 3 : n_3
c_2 = c_0 && ! c_1
	n_4 = c_2 ? n_3 * 2 : n_3
c_3 = c_1 || c_2
assert (c_3 => n_4 % 2 == 0)