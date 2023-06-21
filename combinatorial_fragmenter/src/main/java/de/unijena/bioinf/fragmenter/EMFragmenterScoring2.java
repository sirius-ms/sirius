package de.unijena.bioinf.fragmenter;

import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.ms.AnnotatedPeak;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.ChemistryBase.ms.ft.Fragment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FragmentAnnotation;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import org.openscience.cdk.interfaces.IBond;

import java.util.Arrays;

public class EMFragmenterScoring2 implements CombinatorialFragmenterScoring {
    private static final String[] BondNames = new String[]{
            "C.sp2#423552486:C.sp2#1413433893", "C.sp2#1413433893:C.sp2#423552486", "C.sp2#423552486:C.sp2#423552486", "C.sp2#1413433893:C.sp2#1413433893", "C:C", "C.sp2-C.sp3", "C.sp3-C.sp2", "C.sp3#1187172105-C.sp3#1187172105", "C.sp3-C.sp3", "C.sp2-C.sp2", "C.sp2#-2029203906-C.sp#679726426", "C.sp2#-192120456-C.sp#679726426", "C.sp2#-905092611-C.sp#679726426", "C.sp2-C.sp", "C.sp#679726426-C.sp2#-2029203906", "C.sp#679726426-C.sp2#-192120456", "C.sp#679726426-C.sp2#-905092611", "C.sp-C.sp2", "C.sp3-C.sp", "C.sp-C.sp3", "C-C", "C.sp3#342113914-N.sp3#-403312500", "C.sp3#1680197806-N.sp3#-391791710", "C.sp3#1680197806-N.sp3#-778236959", "C.sp3-N.sp3", "C.sp2#2121648474-N.planar3#1928480249", "C.sp2#-720863186-N.planar3#-1856370315", "C.sp2#851250972-N.planar3#1928480249", "C.sp2#-720863186-N.planar3#335828395", "C.sp2#1124738100-N.planar3#1928480249", "C.sp2-N.planar3", "C.sp2#303431746-N.sp2#-999136859", "C.sp2#-1902313901-N.sp2#-1988899226", "C.sp2-N.sp2", "C.sp2#-1461276143-N.amide#-1876567787", "C.sp2#1232154435-N.amide#-1765892857", "C.sp2#1612184468-N.amide#-1993148702", "C.sp2-N.amide", "C.sp3#1995561937-N.amide#-1765892857", "C.sp3#-303989088-N.amide#-1993148702", "C.sp3-N.amide", "C.sp3#1071601357-N.plus#-1749125445", "C.sp3#-1967232106-N.plus#-1749125445", "C.sp3-N.plus", "C.sp2#476208392-N.sp3#-758578314", "C.sp2#1995580147-N.sp3#-414937772", "C.sp2-N.sp3", "C.sp2#20116721-N.plus.sp2#928285088", "C.sp2#1278434098-N.plus.sp2#928285088", "C.sp2-N.plus.sp2", "C.sp3#-2030939070-N.planar3#1749637777", "C.sp3#2126775190-N.planar3#-1126520694", "C.sp3#-780568915-N.planar3#-954656184", "C.sp3#-2030939070-N.planar3#699053609", "C.sp3-N.planar3", "C.sp3-N.plus.sp2", "C.sp3#1615707026-N.sp2#597609106", "C.sp3-N.sp2", "C.sp3-N.thioamide", "C.sp2#-1461276143-N.thioamide#-1876567787", "C.sp2#1704663761-N.thioamide#-1876567787", "C.sp2-N.thioamide", "C-N", "N.sp3#-403312500-C.sp3#342113914", "N.sp3#-391791710-C.sp3#1680197806", "N.sp3#-778236959-C.sp3#1680197806", "N.sp3-C.sp3", "N.planar3#1928480249-C.sp2#2121648474", "N.planar3#-1856370315-C.sp2#-720863186", "N.planar3#1928480249-C.sp2#851250972", "N.planar3#335828395-C.sp2#-720863186", "N.planar3#1928480249-C.sp2#1124738100", "N.planar3-C.sp2", "N.sp2#-999136859-C.sp2#303431746", "N.sp2#-1988899226-C.sp2#-1902313901", "N.sp2-C.sp2", "N.amide#-1876567787-C.sp2#-1461276143", "N.amide#-1765892857-C.sp2#1232154435", "N.amide#-1993148702-C.sp2#1612184468", "N.amide-C.sp2", "N.amide#-1765892857-C.sp3#1995561937", "N.amide#-1993148702-C.sp3#-303989088", "N.amide-C.sp3", "N.plus#-1749125445-C.sp3#1071601357", "N.plus#-1749125445-C.sp3#-1967232106", "N.plus-C.sp3", "N.sp3#-758578314-C.sp2#476208392", "N.sp3#-414937772-C.sp2#1995580147", "N.sp3-C.sp2", "N.plus.sp2#928285088-C.sp2#20116721", "N.plus.sp2#928285088-C.sp2#1278434098", "N.plus.sp2-C.sp2", "N.planar3#1749637777-C.sp3#-2030939070", "N.planar3#-1126520694-C.sp3#2126775190", "N.planar3#-954656184-C.sp3#-780568915", "N.planar3#699053609-C.sp3#-2030939070", "N.planar3-C.sp3", "N.plus.sp2-C.sp3", "N.sp2#597609106-C.sp3#1615707026", "N.sp2-C.sp3", "N.thioamide-C.sp3", "N.thioamide#-1876567787-C.sp2#-1461276143", "N.thioamide#-1876567787-C.sp2#1704663761", "N.thioamide-C.sp2", "N-C", "N.planar3#-1923447330:C.sp2#-806714497", "N.planar3#-1923447330:C.sp2#851250972", "N.planar3#1928480249:C.sp2#851250972", "N.planar3:C.sp2", "N.sp2:C.sp2", "N.plus.sp2:C.sp2", "N.amide:C.sp2", "N:C", "C.sp2#-806714497:N.planar3#-1923447330", "C.sp2#851250972:N.planar3#-1923447330", "C.sp2#851250972:N.planar3#1928480249", "C.sp2:N.planar3", "C.sp2:N.sp2", "C.sp2:N.plus.sp2", "C.sp2:N.amide", "C:N", "N.planar3#-1856370315:N.sp2#24759605", "N.planar3#-518234509:N.sp2#190264089", "N.planar3:N.sp2", "N.sp2#24759605:N.planar3#-1856370315", "N.sp2#190264089:N.planar3#-518234509", "N.sp2:N.planar3", "N.sp2#446551532:N.sp2#446551532", "N.sp2:N.sp2", "N:N", "C.sp2#1232154435=O.sp2#-160203532", "C.sp2#1612184468=O.sp2#-1006701866", "C.sp2=O.sp2", "C=O", "O.sp2#-160203532=C.sp2#1232154435", "O.sp2#-1006701866=C.sp2#1612184468", "O.sp2=C.sp2", "O=C", "C.sp2#-2035159454-Cl#1751031061", "C.sp2#874021254-Cl#1751031061", "C.sp2#2031693381-Cl#1751031061", "C.sp2-Cl", "C.sp3-Cl", "C-Cl", "Cl#1751031061-C.sp2#-2035159454", "Cl#1751031061-C.sp2#874021254", "Cl#1751031061-C.sp2#2031693381", "Cl-C.sp2", "Cl-C.sp3", "Cl-C", "C.sp2#-1440557769=C.sp2#-1440557769", "C.sp2=C.sp2", "C=C", "C.sp2#293947614-O.planar3#-289266965", "C.sp2#-762012017-O.planar3#-289266965", "C.sp2#-1820888521-O.planar3#-289266965", "C.sp2#1552213789-O.planar3#-289266965", "C.sp2#1361422377-O.planar3#-289266965", "C.sp2#-1366464498-O.planar3#-289266965", "C.sp2-O.planar3", "C.sp2#-554489181-O.sp3#1163511423", "C.sp2#-2038449433-O.sp3#840131337", "C.sp2#-1812822688-O.sp3#1163511423", "C.sp2#2044164800-O.sp3#840131337", "C.sp2-O.sp3", "C.sp3#-304484942-O.sp3#171935693", "C.sp3#-11147740-O.sp3#840131337", "C.sp3#1978230478-O.sp3#1172880295", "C.sp3-O.sp3", "C-O", "O.planar3#-289266965-C.sp2#293947614", "O.planar3#-289266965-C.sp2#-762012017", "O.planar3#-289266965-C.sp2#-1820888521", "O.planar3#-289266965-C.sp2#1552213789", "O.planar3#-289266965-C.sp2#1361422377", "O.planar3#-289266965-C.sp2#-1366464498", "O.planar3-C.sp2", "O.sp3#1163511423-C.sp2#-554489181", "O.sp3#840131337-C.sp2#-2038449433", "O.sp3#1163511423-C.sp2#-1812822688", "O.sp3#840131337-C.sp2#2044164800", "O.sp3-C.sp2", "O.sp3#171935693-C.sp3#-304484942", "O.sp3#840131337-C.sp3#-11147740", "O.sp3#1172880295-C.sp3#1978230478", "O.sp3-C.sp3", "O-C", "C.sp2#-307429614=N.sp2#-999136859", "C.sp2#1995580147=N.sp2#597609106", "C.sp2=N.sp2", "C=N", "N.sp2#-999136859=C.sp2#-307429614", "N.sp2#597609106=C.sp2#1995580147", "N.sp2=C.sp2", "N=C", "O.sp3#321481395-P.ate#536752715", "O.sp3#321481395-P.ate#-170144277", "O.sp3#-694350086-P.ate#-170144277", "O.sp3-P.ate", "O-P", "P.ate#536752715-O.sp3#321481395", "P.ate#-170144277-O.sp3#321481395", "P.ate#-170144277-O.sp3#-694350086", "P.ate-O.sp3", "P-O", "P.ate#536752715=O.sp2#-1610362824", "P.ate#-170144277=O.sp2#-1610362824", "P.ate=O.sp2", "P=O", "O.sp2#-1610362824=P.ate#536752715", "O.sp2#-1610362824=P.ate#-170144277", "O.sp2=P.ate", "O=P", "N.amide-N.amide", "N.sp2-N.thioamide", "N.thioamide-N.sp2", "N.amide-N.sp2", "N.sp2-N.amide", "N.amide-N.sp3", "N.sp3-N.amide", "N.sp3-N.sp3", "N-N", "C.sp2#-1461193439-F#689905106", "C.sp2#441359045-F#689905106", "C.sp2#1464808710-F#689905106", "C.sp2-F", "C.sp3#1096714520-F#-2009324839", "C.sp3-F", "C-F", "F#689905106-C.sp2#-1461193439", "F#689905106-C.sp2#441359045", "F#689905106-C.sp2#1464808710", "F-C.sp2", "F#-2009324839-C.sp3#1096714520", "F-C.sp3", "F-C", "C.sp2#-1366464498:O.planar3#-289266965", "C.sp2#2126682992:O.planar3#-289266965", "C.sp2#1316528246:O.planar3#292217548", "C.sp2:O.planar3", "C.sp2:O.plus.sp2", "C:O", "O.planar3#-289266965:C.sp2#-1366464498", "O.planar3#-289266965:C.sp2#2126682992", "O.planar3#292217548:C.sp2#1316528246", "O.planar3:C.sp2", "O.plus.sp2:C.sp2", "O:C", "C.sp2#207675424-Br#-172846275", "C.sp2#1096896483-Br#-172846275", "C.sp2-Br", "C.sp3-Br", "C-Br", "Br#-172846275-C.sp2#207675424", "Br#-172846275-C.sp2#1096896483", "Br-C.sp2", "Br-C.sp3", "Br-C", "B-C", "C-B", "B-O", "O-B", "N.plus.sp2=O.sp2", "N=O", "O.sp2=N.plus.sp2", "O=N", "N.plus.sp2#928285088-O.minus#-601103932", "N.plus.sp2-O.minus", "N.amide-O.sp3", "N.plus-O.minus", "N.sp2-O.sp3", "N-O", "O.minus#-601103932-N.plus.sp2#928285088", "O.minus-N.plus.sp2", "O.sp3-N.amide", "O.minus-N.plus", "O.sp3-N.sp2", "O-N", "C.sp3-S.inyl", "C.sp2#1360224734-S.3#1803756636", "C.sp2-S.3", "C.sp3#-61517462-S.3#1611167600", "C.sp3#-1798268938-S.3#1611167600", "C.sp3#-1248472974-S.3#1803756636", "C.sp3-S.3", "C.sp3-S.onyl", "C.sp2#1140999339-S.onyl#-570945076", "C.sp2-S.onyl", "C.sp2#-2133101747-S.planar3#-1064027736", "C.sp2-S.planar3", "C.sp2-S.inyl", "C-S", "S.inyl-C.sp3", "S.3#1803756636-C.sp2#1360224734", "S.3-C.sp2", "S.3#1611167600-C.sp3#-61517462", "S.3#1611167600-C.sp3#-1798268938", "S.3#1803756636-C.sp3#-1248472974", "S.3-C.sp3", "S.onyl-C.sp3", "S.onyl#-570945076-C.sp2#1140999339", "S.onyl-C.sp2", "S.planar3#-1064027736-C.sp2#-2133101747", "S.planar3-C.sp2", "S.inyl-C.sp2", "S-C", "S=S", "C.sp#-707249581#N.sp1#1853317954", "C.sp#679726426#N.sp1#1853317954", "C.sp#N.sp1", "C#N", "N.sp1#1853317954#C.sp#-707249581", "N.sp1#1853317954#C.sp#679726426", "N.sp1#C.sp", "N#C", "C.sp2#255402161:S.planar3#1060960655", "C.sp2#-2133101747:S.planar3#-1064027736", "C.sp2#1111002994:S.planar3#1060960655", "C.sp2:S.planar3", "C:S", "S.planar3#1060960655:C.sp2#255402161", "S.planar3#-1064027736:C.sp2#-2133101747", "S.planar3#1060960655:C.sp2#1111002994", "S.planar3:C.sp2", "S:C", "S.onyl#-1656294240=O.sp2#87183595", "S.onyl#-1782870949=O.sp2#87183595", "S.onyl#-1490440810=O.sp2#87183595", "S.onyl#-570945076=O.sp2#87183595", "S.onyl=O.sp2", "S.inyl=O.sp2", "S=O", "O.sp2#87183595=S.onyl#-1656294240", "O.sp2#87183595=S.onyl#-1782870949", "O.sp2#87183595=S.onyl#-1490440810", "O.sp2#87183595=S.onyl#-570945076", "O.sp2=S.onyl", "O.sp2=S.inyl", "O=S", "C.sp2#1704663761=S.2#-2101382639", "C.sp2#-431362313=S.2#-1221395405", "C.sp2=S.2", "C=S", "S.2#-2101382639=C.sp2#1704663761", "S.2#-1221395405=C.sp2#-431362313", "S.2=C.sp2", "S=C", "O-O", "C.sp2-I", "C-I", "I-C.sp2", "I-C", "S.onyl-N.amide", "S.onyl-N.sp3", "S-N", "N.amide-S.onyl", "N.sp3-S.onyl", "N-S", "N.sp2:S.planar3", "N:S", "S.planar3:N.sp2", "S:N", "N-P", "P-N", "P-S", "S-P", "N:O", "O:N", "N.sp2#1239559911=N.sp2#1239559911", "N.sp2=N.sp2", "N=N", "S.onyl-O.sp3", "S-O", "O.sp3-S.onyl", "O-S", "P=S", "S=P", "C.sp3-Si.sp3", "C-Si", "Si.sp3-C.sp3", "Si-C", "C#C", "C.sp3-P.ate", "C-P", "P.ate-C.sp3", "P-C", "P=C", "C=P", "S-Cl", "Cl-S", "C-Se", "Se-C", "S.3-S.3", "S-S", "S-F", "F-S", "P-F", "F-P", "Si-O", "O-Si", "N-Cl", "Cl-N", "N-Se", "Se-N", "N#N", "S=N", "N=S", "O-I", "I-O", "I=O", "O=I", "*~*"
    };
    private static final double[] BondScores = new double[]{
            -0.9420351752895377, -1.6183166543958216, -1.5874481383565404, -1.2041830159484146, -0.9411287131947692, -0.7406860363039007, -0.9493940177870783, -0.8622508371219751, -1.0846716327500727, -0.6261519967776704, -0.4842811523786926, -0.5528929093043589, -0.43339275035148805, -0.5781159485672338, -3, -3, -3, -2.253363515295975, -0.7303582278921903, -1.1866633422203814, -1.2082759424270826, -0.5517750469560385, -0.8018489519642917, -0.3664704197072815, -0.5820658939109833, -0.9475682587134457, -0.16732839971669614, -0.10228913463220322, -0.5869295961237275, -0.6929739367154697, -0.33826677436210323, -0.6656644699980144, -0.04475072225840552, -0.38926180997157156, -0.29083634884611587, -0.7403746787962696, -0.6372282572230068, -0.5607286046255582, -1.1223878778822862, -0.8627291911263444, -0.8342202679092954, -0.7805573201495222, -3, -0.8088829225012972, -0.4181901156986627, -1.0423361886401206, -0.4389084176788948, -0.3285412607577297, -0.3690514921670866, -0.4058661612458242, -1.0518668227431154, -0.7974283059069359, -3, -1.0250497198831763, -0.7641656041668686, -0.875651738645004, -0.8604409907269156, -0.6714471998382647, -0.4424658940630287, -0.11341693468856189, -0.8501066510091939, -0.2617339125645116, -0.812282572970741, -3, -0.1477226146389451, -0.429684016144294, -0.6800801165304446, -0.8992635791388908, -1.5146584150336466, -2.0015301054532304, -1.2496874278053016, -1.5064009474476565, -0.7636463572059365, -3, -2.3613500243522663, -0.8953510175183028, -1.9831813421254034, -0.5955863894919298, -0.609064243605189, -0.636072674883421, -1.7116805360614706, -1.256665446345366, -1.0607282611491535, -0.551174152635908, -0.9789447217556597, -0.7059461764305739, -3, -3, -1.2816171351320835, -3, -3, -3, -0.32865446998421693, -0.37608884335984333, -0.6400010164920202, -0.34917322147188873, -0.40839062784995717, -0.6441613431899156, -0.8539190397629118, -0.5905715947339094, -0.5461543309015358, -1.9161906599805376, -0.21888843094001806, -0.4407337461702662, -1.0451967487372575, -1.1114289757255755, -2.8152153832787112, -2.9101350589423265, -0.8305219793868389, -0.7766054523138262, -1.9252724135971138, -1.7121903737346815, -0.3010299956639812, -0.6543171476957267, -0.21394573699490071, -0.1694434830394054, -0.469647754729853, -0.3602794786583807, -0.4211676691918578, -0.471414280588265, -0.3010299956639812, -0.01, -0.30941264524085177, -0.2975892037641053, -0.9249353201472074, -0.489904088663762, -0.9198525811803546, -0.3704514044224498, -0.4590830509938043, -0.5484328497527258, -1.7428619355621686, -1.4406444498797712, -0.7889192386887659, -1.2041199826559248, -3, -3, -3, -1.6812412373755872, -0.5146856227737431, -0.3904676709957563, -0.3910290989953079, -0.38221639500225457, -0.5058115085040265, -0.3010299956639812, -3, -3, -3, -3, -3, -0.3010299956639812, -0.8600590178289075, -0.9443408563300524, -0.9533194602461462, -0.12641844666634777, -0.7082999975664268, -1.1611521608300535, -0.348448636051428, -0.3568429304700213, -0.1856993740915889, -0.3346602686203016, -0.579874417268273, -0.732761371506253, -0.726193994780739, -0.42210510351989244, -0.5164639920197484, -1.4154464299625187, -3, -1.405500809814344, -0.7319371651528097, -0.5158738437116791, -0.3784422189727593, -0.37087867846969946, -0.4338406532750116, -0.4514131605424432, -1.632690846630742, -1.6647508296858886, -0.6295882866372565, -3, -3, -3, -3, -1.7470843738277957, -1.6601284975782327, -0.6200839377934404, -3, -1.2333420142540432, -1.9138138523837167, -2.533772058384718, -3, -0.4746276519168391, -0.8634753779383195, -0.5043882806995084, -0.6217878381542667, -0.8288632858849396, -1.0231762208058315, -3, -3, -0.685389336684271, -0.8532147720421492, -0.9864271928107261, -1.706874469930895, -1.4156477184198344, -1.2518119729937995, -0.7117397114727962, -0.532168821050568, -2.33075187728367, -2.2334720495980367, -1.0733603840225379, -0.9378520932511555, -3, -3, -3, -1.414973347970818, -0.443255330712335, -0.04635498165622228, -0.24048086083550096, -0.3858193759961842, -0.2815721796848198, -0.4212501729117958, -0.8121446936423681, -0.5743564601666518, -0.38365999650332894, -0.5589088309267938, -0.5415927841259115, -0.45281149272909427, -0.38304053173047026, -0.6580678603623021, -0.7186440620141142, -0.3010299956639812, -3, -3, -3, -3, -3, -3, -0.3010299956639812, -0.056211643716185845, -0.18196359378938654, -0.7633922506320169, -0.2645015097451637, -0.977969316459138, -0.3010299956639812, -1.4848204721546754, -0.6166338619229842, -2.6074550232146687, -1.2392796124961212, -0.9733244114133744, -0.3010299956639812, -0.2752539549256701, -0.2783160426061124, -0.33063142223063663, -0.3743678562162679, -1.1026623418971477, -3, -3, -3, -3, -2.0569048513364727, -3, -0.24809392047618273, -0.7569619513137056, -3, -0.6908635308846854, -0.47998789537126557, -3, -2.6589648426644352, -0.5265149722141023, -0.4607308385314931, -0.5759243386837973, -0.33555677572837905, -0.3678104208099309, -0.42442813299124194, -3, -3, -1.7410202134380155, -3, -1.0944212126043618, -1.0131614460579708, -0.6493174698918195, -0.5062693573319154, -0.3789268458566023, -0.746993268393257, -3, -0.8359159964762619, -0.5835343681296516, -0.7997409082872117, -0.20360934828562963, -0.22422555721317727, -0.17985956453637314, -0.35723251084800406, -0.3322235158469251, -0.9802559418042428, -0.687811627170228, -1.4739733021220007, -0.8969280890918803, -2.048023264057238, -1.44656106288822, -0.3569814009931312, -0.6721666209423166, -0.9180893138517614, -1.480326394594874, -1.3288539254732816, -1.0354922760897471, -0.3979400086720376, -1.335314592824067, -0.46222969931831737, -1.255272505103306, -0.8725555696286635, -0.7865977167884788, -0.5618800812720213, -2.1271047983648077, -3, -3, -3, -2.1271047983648077, -0.7760996084745277, -0.03550823223215843, -0.3840042307287451, -0.3679054503160494, -1.2041199826559248, -0.5757640457669937, -1.105088416763723, -0.9023735263149031, -0.3991616535898187, -1.2041199826559248, -1.1270216877565924, -2.7307822756663893, -0.9685428140832743, -1.1127570787424328, -1.02444245646766, -0.5694142734314143, -0.3010299956639812, -3, -3, -3, -3, -3, -3, -0.3010299956639812, -0.4230945028409282, -0.6013053525943808, -0.5168512336024754, -0.738122128298167, -3, -3, -3, -2.462397997898956, -0.8355944992695135, -0.30915588033584573, -0.32516367538070057, -3, -1.3921104650113136, -0.5065308840362915, -0.5708166380846323, -0.4156032168004475, -0.7833524440694514, -0.53443155072786, -0.374956101751348, -0.2986971638315097, -1.2041199826559248, -0.4857838071886541, -1.2041199826559248, -0.49114937680149195, -0.399535703603384, -0.4052922069260596, -0.7010579625976836, -0.24333911939637845, -0.6623366830202793, -0.452182059525319, -0.40350796291097984, -1.03698356625317, -1.1560410362494977, -0.7067953418479754, -1.1649044965808029, -1.0492180226701815, -0.7332947860422265, -3, -1.8504930719009631, -0.6553055032811875, -0.7623569832004118, -0.36382082552343664, -0.8238602137101319, -0.4856407373679896, -0.3444956894450715, -0.3099237088239637, -0.5964561138099639, -0.27470105694163205, -3, -0.7346855566025534, -3, -0.23408320603336794, -3, -0.5429587347256852, -0.5735045308749096, -0.7201593034059569, -3, -1.0881360887005513, -3, -0.4903227113246148, -0.5325203911190697, -0.3357921019231931, -3, -0.01, -3, -3, -0.5195251032727498, -3, -0.23408320603336794, -0.23408320603336794, -0.3010299956639812, -3, -3
    };




    private final static TObjectDoubleHashMap<String> name2score;

    static {
        name2score = new TObjectDoubleHashMap<>();
        for (int j = 0; j < BondNames.length; ++j) {
            name2score.put(BondNames[j], BondScores[j]);
        }
    }

    public static double scoreFor(IBond b) {
        final String ecfp = DirectedBondTypeScoring.bondNameEcfp(b, true);
        if (name2score.containsKey(ecfp)) return name2score.get(ecfp);
        final String specific = DirectedBondTypeScoring.bondNameSpecific(b, true);
        if (name2score.containsKey(specific)) return name2score.get(specific);
        final String generic = DirectedBondTypeScoring.bondNameGeneric(b, true);
        if (name2score.containsKey(generic)) return name2score.get(generic);
        return name2score.get("*~*");
    }

    public static int scoreLevelFor(IBond b) {
        final String ecfp = DirectedBondTypeScoring.bondNameEcfp(b, true);
        if (name2score.containsKey(ecfp)) return 2;
        final String specific = DirectedBondTypeScoring.bondNameSpecific(b, true);
        if (name2score.containsKey(specific)) return 1;
        final String generic = DirectedBondTypeScoring.bondNameGeneric(b, true);
        if (name2score.containsKey(generic)) return 0;
        return -1;
    }

    // the prior penalty for hydrogen missmatch PER hydrogen
    protected static double rearrangementScore = -0.25d;
    // the prior score for explaining a peak
    protected static double peakScore = 6d;
    // penalizes deep nodes, such that on a tie, a node with fewer cuts is always preferred
    protected static double depthPenalty = -0.05f;

    private double[] bondScoresLeft, bondScoresRight;
    private TObjectDoubleHashMap<MolecularFormula> fragmentScores;

    public EMFragmenterScoring2(MolecularGraph graph, FTree tree) {
        this.bondScoresLeft = new double[graph.bonds.length];
        this.bondScoresRight = new double[graph.bonds.length];
        this.fragmentScores = tree == null ? null : new TObjectDoubleHashMap<>(tree.numberOfVertices(), 0.75f, 0);
        if (tree != null) {
            FragmentAnnotation<AnnotatedPeak> ano = tree.getFragmentAnnotationOrThrow(AnnotatedPeak.class);
            for (Fragment f : tree) {
                final double intensityScore = ano.get(f).getRelativeIntensity();
                fragmentScores.adjustOrPutValue(f.getFormula().withoutHydrogen(), intensityScore, intensityScore);
            }
        }
        final double wildcard = name2score.get("*~*");
        for (int i = 0; i < bondScoresLeft.length; ++i) {
            IBond b = graph.bonds[i];

            {
                String ecfpName = DirectedBondTypeScoring.bondNameEcfp(b, true);
                if (name2score.containsKey(ecfpName)) {
                    bondScoresLeft[i] = name2score.get(ecfpName);
                } else {
                    String name = DirectedBondTypeScoring.bondNameSpecific(b, true);
                    if (name2score.containsKey(name)) {
                        bondScoresLeft[i] = name2score.get(name);
                    } else {
                        name = DirectedBondTypeScoring.bondNameGeneric(b, true);
                        if (name2score.containsKey(name)) {
                            bondScoresLeft[i] = name2score.get(name);
                        } else {
                            bondScoresLeft[i] = wildcard;
                        }
                    }
                }
            }
            {
                String ecfpName = DirectedBondTypeScoring.bondNameEcfp(b, false);
                if (name2score.containsKey(ecfpName)) {
                    bondScoresRight[i] = name2score.get(ecfpName);
                } else {
                    String name = DirectedBondTypeScoring.bondNameSpecific(b, false);
                    if (name2score.containsKey(name)) {
                        bondScoresRight[i] = name2score.get(name);
                    } else {
                        name = DirectedBondTypeScoring.bondNameGeneric(b, false);
                        if (name2score.containsKey(name)) {
                            bondScoresRight[i] = name2score.get(name);
                        } else {
                            bondScoresRight[i] = wildcard;
                        }
                    }
                }
            }
            if (bondScoresLeft[i] >= 0) bondScoresLeft[i] = -1e-3;
            if (bondScoresRight[i] >= 0) bondScoresRight[i] = -1e-3;
        }

    }

    /**
     * score for cutting the bond. direction is true when the bond goes from fragment to loss and
     * false when the bond goes from loss to fragment
     *
     * @return
     */
    @Override
    public double scoreBond(IBond bond, boolean direction) {
        return (direction ? bondScoresLeft[bond.getIndex()] : bondScoresRight[bond.getIndex()]);
    }

    public double peakIntensityScore(float peakIntensity) {
        /*
        if (peakIntensity > 0.1) return 1;
        if (peakIntensity > 0.01) return 0.5;
        return 0;
         */
        if (peakIntensity > 0.01) return 0.5;
        if (peakIntensity > 0.05) return 1;
        if (peakIntensity > 0.1) return 2;
        if (peakIntensity > 0.25) return 3;
        return 0;
    }

    @Override
    public double scoreFragment(CombinatorialNode fragment) {
        if (fragment.fragment.isInnerNode()) {
            return fragment.depth * -0.05;
        } else {
            return terminalScore(fragment);
        }
    }

    public double terminalScore(CombinatorialNode fragment) {
        return peakIntensityScore(fragment.fragment.peakIntensity) + peakScore;
    }

    @Override
    public double scoreEdge(CombinatorialEdge edge) {
        CombinatorialFragment sourceFragment = edge.source.fragment;
        CombinatorialFragment targetFragment = edge.target.fragment;
        double depthScore = (edge.source.depth+1) * depthPenalty;

        if (targetFragment.isInnerNode()) {
            return scoreBond(edge.getCut1(), edge.getDirectionOfFirstCut()) + (edge.getCut2() != null ? scoreBond(edge.getCut2(), edge.getDirectionOfSecondCut()) : 0);
        } else {
            int hydrogenDiff = Math.abs(sourceFragment.hydrogenRearrangements(targetFragment.getFormula()));
            if (hydrogenDiff == 0) {
                return depthScore;
            } else {
                double score = hydrogenDiff * rearrangementScore + depthScore;
                return (Double.isNaN(score) || Double.isInfinite(score)) ? (-1.0E6) : score;
            }
        }
    }
}
