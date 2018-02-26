(ns nutrition-noter.core-test
  (:require [nutrition-noter.core :as core]
            [cljsjs.moment]
            [cljs.test :refer-macros [deftest is]]))

(deftest dissoc-middle-idx-test
  (is (= (core/dissoc-idx [1 2 3] 1) [1 3])))

(deftest sanitize-valid-file-name
  (is (= (core/sanitize-file-name "valid") "valid")))

(deftest sanitize-long-file-name
  (is (<= (count (core/sanitize-file-name "Lorem_ipsum_dolor_sit_amet,_nulla_reformidans_ad_vis._Mel_sale_civibus_expetendis_no._Modo_mandamus_salutatus_cu_vel._Ei_labores_nostrum_similique_quo,_ignota_moderatius_no_pri._Ad_sensibus_efficiantur_qui,_eos_sumo_vitae_maluisset_in._Sea_cu_omnes_vitae_dolore.")) 256)))

(deftest convert-empty-to-local
  (let [expected-str "2018-04-22T23:15:00.000" offset "-04:00"]
    (is (= expected-str
           (core/convert-universal-to-local ""
                                            (js/moment (str expected-str offset))
                                            offset)))))

(deftest convert-valid-to-local
    (is (= "2018-04-22T23:15:00.000"
           (core/convert-universal-to-local "2018-04-23T03:15:00.000Z"
                                            nil
                                            "-04:00"))))
