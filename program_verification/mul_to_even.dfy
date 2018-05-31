method Mul_to_even(n1: int, n2: int) returns (n3:int) {
    n3 := n1 + n2;
    if (n3 % 2 == 0) {
      n3 := n3 * 3;
    } else {
      n3 := n3 * 2;
    }
    assert n3 % 2 == 0;
}