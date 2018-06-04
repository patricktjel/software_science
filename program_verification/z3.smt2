;n_3 = c_0 ? n_1 + n_2 : n_0
;c_1 = c_0 && (n_3 % 2 == 0)
;	n_4 = c_1 ? n_3 * 3 : n_0
;c_2 = c_0 && ! c_1
;	n_4 = c_2 ? n_3 * 2 : n_0
;c_3 = c_1 || c_2
;assert (c_3 && n_4 % 2 == 0)

(declare-fun n_0 () Int)
(declare-fun n_1 () Int)
(declare-fun n_2 () Int)
(declare-fun n_3 () Int)
(declare-fun n_4 () Int)
(declare-fun c_0 () Bool)
(declare-fun c_1 () Bool)
(declare-fun c_2 () Bool)
(declare-fun c_3 () Bool)

(assert (= c_0 true)); setup path conditions
(assert (= n_3 (ite c_0 (+ n_1 n_2) n_0))); line 1
(assert (= c_1 (and c_0 (= (mod n_3 2) 0)))); line 2 if
(assert (= n_4 (ite c_1 (* n_3 3) n_3))); line 3 n_4 = 
(assert (= c_2 (and c_0 (not c_1)))); else
(assert (= n_4 (ite c_2 (* n_3 2) n_3))); line 5 n_4 =
(assert (= c_3 (or c_1 c_2))); end if
(push) ; asserts could be anywhere so push needs to be done 
(assert (and c_3 (not (= (mod n_4 2) 0)))) ; final assert
(check-sat)
(pop)