/*
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2015 Kai Dührkop
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.unijena.bioinf.MassDecomposer.cli;

import com.lexicalscope.jewel.cli.ArgumentValidationException;
import com.lexicalscope.jewel.cli.Cli;
import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.HelpRequestedException;
import de.unijena.bioinf.ChemistryBase.chem.*;
import de.unijena.bioinf.ChemistryBase.ms.Deviation;
import de.unijena.bioinf.MassDecomposer.ChemicalValidator;
import de.unijena.bioinf.MassDecomposer.Chemistry.MassToFormulaDecomposer;
import de.unijena.bioinf.MassDecomposer.DecompositionValidator;
import de.unijena.bioinf.MassDecomposer.Interval;
import de.unijena.bioinf.MassDecomposer.ValenceValidator;

import java.io.PrintStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;

public class Main {

    public final static String VERSION = "1.21";

    public final static String CITATION =
            "DECOMP---from interpreting Mass Spectrometry peaks to solving the Money Changing Problem\n" +
                    "Sebastian Böcker, Zsuzsanna Lipták, Marcel Martin, Anton Pervukhin and Henner Sudek\n" +
                    "Bioinformatics, 24(4):591-593, 2008\n\n" +
                    "Faster mass decomposition\n" +
                    "Kai Dührkop, Marcus Ludwig, Marvin Meusel and Sebastian Böcker\n" +
                    "Proc. of Workshop on Algorithms in Bioinformatics (WABI 2013), of Lect Notes Comput Sci, Springer, Berlin, 2013";

    public final static String VERSION_STRING = "DECOMP " + VERSION + "\n\n" + CITATION;

    public final static String USAGE = "usage:\ndecomp <mass>\ndecomp -p <ppm> -a <absolute error> -e \"CH[<min>-<max>]N[<min>-]O[-<max>]P[<num>]\" <mass>";


    public static void main2(String[] args) {
        final double[] values = new double[]{
                146.0690951d, 147.053073d, 165.0790735d, 180.0633665d, 192.0268535d, 192.0268535d, 204.0898696d, 206.0425975d,
                206.0426059d, 209.0535235d, 209.0535235d, 216.0399638d, 226.0687999d, 243.0856935d, 244.069497d, 246.0506023d,
                250.1318535d, 254.0636804d, 264.0083133d, 264.1473748d, 266.0058601d, 267.0960649d, 267.1946765d, 272.0682743d,
                276.0846135d, 276.0846435d, 278.0402734d, 283.0907435d, 284.0683878d, 286.1543685d, 288.0633076d, 290.0988046d,
                294.095005d, 294.095076d, 302.0787015d, 303.1430235d, 306.1692335d, 307.0835735d, 307.0836935d, 308.0508425d,
                310.1261122d, 313.1313585d, 314.0790053d, 318.0943868d, 322.0900093d, 323.1214762d, 324.0357921d, 324.1056035d,
                326.1000418d, 328.3202235d, 329.1259722d, 329.1261731d, 336.0604652d, 338.085084d, 339.1165235d, 340.0552741d,
                342.0951631d, 342.1160683d, 343.126437d, 347.0630447d, 347.0630809d, 349.2100625d, 351.2619235d, 352.3201235d,
                353.0878175d, 353.1684224d, 354.0949165d, 354.095048d, 354.0950742d, 356.1106082d, 359.1426898d, 360.0457352d,
                361.1372041d, 362.0847769d, 363.0579585d, 363.1166341d, 367.1476838d, 369.1632235d, 371.1214414d, 372.1053758d,
                372.1053758d, 372.1055759d, 372.12682d, 373.1007561d, 373.1008168d, 373.1372235d, 374.106007d, 376.0773182d,
                378.0928115d, 379.1113436d, 379.2935235d, 380.0810622d, 380.1084682d, 384.1268261d, 386.0615721d, 386.1940822d,
                387.1528836d, 388.1217662d, 388.1372462d, 388.1727498d, 388.1727607d, 392.0396007d, 393.0879963d, 393.0884555d,
                394.0869275d, 396.1033703d, 398.1005443d, 399.2368009d, 400.0530747d, 401.0806235d, 402.1155716d, 402.152361d,
                403.2205235d, 404.1674395d, 404.1680506d, 405.0875439d, 405.1996014d, 406.0358593d, 406.0359765d, 406.1320751d,
                407.1790235d, 408.1796865d, 408.2988235d, 409.0830693d, 409.0830866d, 412.0851862d, 413.2777235d, 415.0694247d,
                415.3449125d, 416.2052389d, 419.1790806d, 420.0674826d, 421.194676d, 422.0097728d, 430.1368751d, 431.3398235d, 432.1053433d, 432.1054476d, 432.1528888d, 432.1635671d, 432.1991477d, 432.2508856d, 434.0666447d, 434.1208012d, 434.120817d, 434.1209789d, 434.1209852d, 434.1210203d, 434.1210613d, 434.1210941d, 434.1211097d, 434.1212347d, 435.2102715d, 435.2103384d, 439.1688363d, 440.0929524d, 443.2154235d, 444.1989308d, 445.1799195d, 446.0848185d, 446.1783745d, 448.1003042d, 448.1003112d, 448.1004615d, 448.1427501d, 448.2308981d, 449.1896433d, 450.0413952d, 450.1157471d, 450.1158357d, 450.1158837d, 450.1159295d, 450.1159423d, 450.1159795d, 450.1160359d, 455.1273438d, 456.1479235d, 456.199303d, 456.1993145d, 458.1036472d, 460.1001306d, 461.2257912d, 463.1688902d, 463.2052235d, 464.0955424d, 464.1315838d, 465.257287d, 467.1425407d, 472.1940164d, 473.2257899d, 475.1538296d, 475.2052235d, 475.2699322d, 476.1316262d, 486.158432d, 486.1710042d, 488.0422877d, 488.1853526d, 489.1695908d, 489.2205506d, 489.2207306d, 492.0385122d, 492.1476445d, 494.1768303d, 499.0567068d, 499.1105315d, 502.1079305d, 502.1080019d, 502.1537365d, 503.3246617d, 504.1260958d, 504.1266013d, 504.1826619d, 506.1421083d, 510.137115d, 510.2806317d, 512.1893138d, 514.1175548d, 516.1260373d, 516.1260429d, 516.1260551d, 516.1260955d, 520.1209421d, 520.121164d, 522.1301396d, 523.1686402d, 524.1376079d, 527.1266104d, 530.2722347d, 530.3096743d, 532.251962d, 533.153173d, 533.1531751d, 534.1432628d, 534.1475398d, 536.1161346d, 536.1591232d, 537.1481235d, 537.2209235d, 538.0705533d, 538.1082541d, 542.103259d, 542.1036076d, 546.0686074d, 547.3872235d, 548.1134099d, 550.0957939d, 550.131866d, 550.1683279d, 550.2037604d, 552.1628222d, 553.1431235d, 555.141485d, 555.141485d, 555.141485d, 555.1415501d, 556.1349202d, 557.2836537d, 558.1562808d, 560.1527945d, 563.3218474d, 564.1083097d, 566.1197236d, 566.1500235d, 566.1631781d, 566.1988964d, 566.1997971d, 567.1588235d, 568.1848176d, 569.2682235d, 571.1357293d, 572.1231326d, 574.0635921d, 574.1474114d, 575.3822235d, 576.0805492d, 576.0807933d, 577.1220319d, 577.1795235d, 577.3837235d, 577.3974854d, 577.3974854d, 578.1240762d, 578.256853d, 579.1518546d, 579.1519049d, 580.1137133d, 580.1786024d, 580.1786698d, 582.1523517d, 583.0812235d, 584.113781d, 588.1454107d, 589.3980235d, 591.4134235d, 593.3926235d, 594.1581235d, 594.2520083d, 595.4084235d, 596.0456842d, 596.1524622d, 596.1525239d, 596.1525323d, 596.1531133d, 596.1631784d, 596.1735337d, 596.1736043d, 596.1736401d, 596.173734d, 596.1737533d, 596.173931d, 596.1740807d, 596.1741066d, 598.0624223d, 598.0682237d, 598.1891884d, 598.2468774d, 601.1328096d, 604.1031492d, 604.134117d, 604.285103d, 610.1530392d, 610.1531712d, 610.1959623d, 611.1561559d, 612.1470136d, 612.1479601d, 612.1479979d, 612.1583446d, 612.1684665d, 613.1319652d, 613.1795235d, 613.2001528d, 614.0377737d, 614.1844527d, 616.1405818d, 616.2725649d, 617.1825795d, 618.200653d, 618.215206d, 620.0453136d, 620.1545439d, 620.1567007d, 621.1776235d, 624.1684437d, 626.1480963d, 626.1631895d, 626.1995657d, 628.1996787d, 629.1271128d, 629.1744262d, 629.1952453d, 630.1345336d, 630.1902162d, 630.1905852d, 630.1918711d, 632.1358235d, 633.2996235d, 634.1505405d, 634.1658903d, 635.4033235d, 636.2528235d, 638.1844327d, 639.1984235d, 640.1096159d, 640.1641705d, 640.2420749d, 641.1408054d, 641.141879d, 643.2113235d, 643.3126235d, 644.1947324d, 646.1299766d, 648.1251814d, 649.3826235d, 650.1395116d, 651.3806235d, 652.3215235d, 654.0896424d, 654.0897257d, 654.089852d, 654.0900215d, 656.1584571d, 658.1286463d, 658.1633857d, 658.173947d, 658.2525235d, 658.3063963d, 658.3179235d, 659.1670235d, 660.2795433d, 661.2062909d, 662.0912072d, 662.1452868d, 662.1629549d, 663.1087654d, 664.1606833d, 665.3768879d, 665.3769529d, 666.1619397d, 666.17672d, 668.2310725d, 669.2267485d, 669.4089235d, 670.1739262d, 672.0811831d, 672.1898385d, 674.0838726d, 674.2784222d, 675.2088873d, 676.274096d, 678.1112235d, 678.1122707d, 678.1400963d, 678.1579196d, 678.199815d, 678.2699235d, 678.2887206d, 678.2900235d, 679.1607063d, 679.1901235d, 679.4294235d, 680.1720612d, 680.194565d, 681.2329235d, 682.1373302d, 682.1453434d, 682.1739216d, 682.1741292d, 683.3877235d, 684.1743908d, 684.1896262d, 685.2582235d, 687.1829439d, 689.1807235d, 690.3442235d, 691.3053235d, 692.3246093d, 694.1262536d, 694.1713965d, 694.1737666d, 694.2407775d, 694.2846523d, 694.3567138d, 695.1849397d, 697.2219614d, 698.1690812d, 699.2014174d, 700.1401301d, 700.2717235d, 701.2593494d, 702.1618235d, 703.2038287d, 704.1553194d, 704.1562917d, 705.1741934d, 706.2142205d, 708.1540225d, 708.1894673d, 708.3000425d, 710.4241235d, 712.1604235d, 712.1844752d, 712.2508645d, 712.2522397d, 712.3393235d, 714.2728641d, 715.0475642d, 715.1951235d, 718.1770765d, 719.3366479d, 720.1213235d, 720.2104625d, 722.1948038d, 723.3678235d, 724.0450424d, 724.1444142d, 725.2165235d, 725.3108235d, 726.2001243d, 728.2064922d, 730.1715286d, 730.2381834d, 733.3782258d, 737.4356235d, 739.4503235d, 741.2033651d, 741.2051235d, 742.1586559d, 742.1845349d, 742.1950531d, 742.2673954d, 743.4453646d, 744.1995888d, 746.1454646d, 748.1826147d, 756.1159429d, 756.2110502d, 758.2051073d, 760.2209858d, 760.2420612d, 760.2426442d, 761.1690235d, 762.176628d, 771.4408235d, 772.1803235d, 772.2056057d, 772.2057808d, 772.2059231d, 774.2001662d, 774.2004334d, 776.3459577d, 777.26915d, 778.1714575d, 778.3256579d, 780.1685469d, 780.3213571d, 782.2097409d, 786.1163992d, 786.1332101d, 786.1752435d, 786.1759388d, 788.2158394d, 789.4514235d, 790.090443d, 791.2849376d, 792.1339863d, 794.1878138d, 795.3523235d, 796.242329d, 797.4568474d, 798.3279016d, 804.2199326d, 806.0541375d, 806.0548297d, 806.3107235d, 806.3374735d, 808.1576794d, 808.1826284d, 809.3686235d, 810.1829676d, 810.2573635d, 811.4351235d, 812.2370536d, 812.2632837d, 814.1268265d, 816.205421d, 817.2716235d, 818.1587665d, 818.211033d, 820.1554562d, 820.2162357d, 821.2951556d, 824.1980429d, 826.1483591d, 826.2528396d, 827.4301235d, 828.3197235d, 829.4457235d, 830.1233022d, 830.2262389d, 831.287682d, 831.4616235d, 831.4646235d, 832.1194458d, 838.1896148d, 840.2682723d, 840.3131212d, 840.3421669d, 842.2113362d, 846.2216362d, 846.234583d, 851.3056023d, 852.1839035d, 853.430613d, 856.1886934d, 856.2634103d, 858.2064635d, 862.2163347d, 862.2293811d, 862.3247235d, 864.362659d, 865.2825235d, 866.2266332d, 868.2115235d, 868.2422235d, 870.2036295d, 870.46055d, 871.4986235d, 872.2372901d, 872.4766235d, 873.2461016d, 878.2454679d, 880.1868419d, 881.2406649d, 882.2784586d, 885.2686235d, 885.4205235d, 886.1978923d, 888.1984235d, 888.2319752d, 888.2321655d, 890.2245818d, 890.4881235d, 894.2404914d, 900.1927235d, 902.2275586d, 902.247318d, 902.2693663d, 903.1366501d, 903.2506483d, 903.2564902d, 904.2478435d, 905.4982556d, 906.2790728d, 908.2943752d, 909.2354235d, 910.2137806d, 910.2163829d, 914.2478739d, 917.2583235d, 917.4981936d, 918.2422996d, 920.3941235d, 922.2141866d, 922.274183d, 922.51373d, 924.2507864d, 924.2895667d, 926.2231728d, 929.4980704d, 932.2583781d, 934.2092648d, 934.4766526d, 936.2318053d, 936.2320623d, 939.3005235d, 939.5402994d, 940.2456507d, 941.3980235d, 943.2678302d, 944.2648235d, 945.4928104d, 946.1695998d, 948.1667977d, 948.2528723d, 948.2894882d, 950.2481235d, 950.3047471d, 950.4717667d, 952.2271437d, 952.2412417d, 954.2437186d, 959.4743413d, 961.4151235d, 962.1649746d, 964.4508881d, 965.3380235d, 966.3002536d, 967.4986047d, 968.3166942d, 968.3897235d, 970.2364235d, 971.4204286d, 971.4204959d, 972.3102928d, 972.3258653d, 972.4531151d, 974.2684588d, 975.503492d, 976.1799863d, 976.2957706d, 978.2995778d, 981.4776344d, 982.2375944d, 983.3477364d, 985.5244871d, 986.448643d, 987.4156235d, 987.5394611d, 988.3205392d, 990.4509853d, 994.4617411d, 1002.225028d, 1002.395198d, 1003.534448d, 1003.534517d, 1014.410382d, 1017.547863d, 1017.550347d, 1018.459624d, 1019.312154d, 1023.432624d, 1029.514287d, 1031.529753d, 1033.545175d, 1036.471792d, 1040.242956d, 1040.250506d, 1043.378724d, 1043.493197d, 1043.52999d, 1045.544143d, 1045.545665d, 1046.221281d, 1047.560816d, 1049.539044d, 1049.539049d, 1049.539368d, 1049.53987d, 1049.540469d, 1051.545276d, 1051.554624d, 1051.555864d, 1053.499192d, 1054.235724d, 1056.54816d, 1057.269012d, 1061.540574d, 1061.541594d, 1062.245569d, 1063.554693d, 1064.306018d, 1065.536257d, 1067.464149d, 1067.550553d, 1067.550981d, 1068.547634d, 1069.519605d, 1069.566408d, 1071.521787d, 1075.519379d, 1075.555165d, 1079.513678d, 1087.447d, 1089.259724d, 1089.534472d, 1089.53529d, 1091.550859d, 1098.54494d, 1099.5285d, 1099.576999d, 1105.529326d, 1105.529351d, 1105.529564d, 1105.530196d, 1107.545693d, 1107.546289d, 1109.561362d, 1115.468456d, 1117.52704d, 1119.545246d, 1121.524369d, 1121.525311d, 1121.561424d, 1121.561424d, 1123.540777d, 1123.575905d, 1124.328783d, 1125.556565d, 1127.52609d, 1128.554084d, 1129.527735d, 1131.542728d, 1133.473724d, 1135.284724d, 1135.492997d, 1135.538659d, 1135.576756d, 1137.559004d, 1139.535724d, 1141.470883d, 1143.522961d, 1145.522304d, 1147.537446d, 1151.314724d, 1159.389724d, 1159.538697d, 1165.573978d, 1171.516323d, 1173.406724d, 1173.482415d, 1175.537111d, 1179.474433d, 1188.318332d, 1189.514207d, 1192.306724d, 1194.566989d, 1195.597781d, 1196.582939d, 1199.589056d, 1203.532182d, 1205.493562d, 1209.507164d, 1211.630724d, 1212.575057d, 1212.579724d, 1213.512839d, 1215.525039d, 1220.306976d, 1223.589817d, 1225.519537d, 1227.52717d, 1227.586334d, 1227.586768d, 1227.586835d, 1229.604724d, 1229.604724d, 1231.618624d, 1231.61899d, 1233.637724d, 1239.305969d, 1239.520182d, 1239.588411d, 1239.588411d, 1243.526418d, 1245.597702d, 1250.58923d, 1251.618461d, 1253.51366d, 1253.605456d, 1253.606647d, 1257.598058d, 1258.582547d, 1259.568918d, 1260.599666d, 1267.514728d, 1267.580628d, 1267.587649d, 1269.597177d, 1269.597841d, 1269.598029d, 1283.524976d, 1283.577133d, 1285.589235d, 1287.607829d, 1295.577763d, 1295.625724d, 1297.632004d, 1297.632124d, 1299.609756d, 1302.373724d, 1302.503125d, 1311.353327d, 1311.522672d, 1319.393724d, 1326.609724d, 1327.523051d, 1328.625636d, 1337.589923d, 1337.590362d, 1343.634071d, 1344.621724d, 1347.668724d, 1359.631988d, 1361.646248d, 1361.646975d, 1361.649492d, 1363.661412d, 1363.662916d, 1368.620724d, 1390.620713d, 1391.657724d, 1431.650514d, 1431.651103d, 1431.651626d, 1431.652754d, 1458.615036d, 1489.568687d
        };

        final MassToFormulaDecomposer decomposer = new MassToFormulaDecomposer(ChemicalAlphabet.alphabetFor(MolecularFormula.parse("CHNOPS")));
        final Deviation dev = new Deviation(1);
        final PeriodicTable t = PeriodicTable.getInstance();
        final HashMap<Element, Interval> map = new HashMap<Element, Interval>();
        map.put(t.getByName("C"), new Interval(0, 100));
        map.put(t.getByName("H"), new Interval(0, 200));
        map.put(t.getByName("O"), new Interval(0, 50));
        map.put(t.getByName("N"), new Interval(0, 10));
        map.put(t.getByName("P"), new Interval(0, 10));
        map.put(t.getByName("S"), new Interval(0, 10));
        final long time = System.nanoTime();
        for (double v : values) {
            decomposer.decomposeToFormulas(v, dev, map);
        }
        final long diff = System.nanoTime() - time;
        System.out.println(diff);
        System.out.println(values.length);

    }

    public static void main(String[] args) {
        final Cli<Options> cli = CliFactory.createCli(Options.class);
        final Options options;
        try {
            options = cli.parseArguments(args);
        } catch (ArgumentValidationException e) {
            final PrintStream out;
            if (e instanceof HelpRequestedException) {
                out = System.out;
            } else {
                out = System.err;
                out.println("Error while parsing command line arguments: " + e.getMessage());
            }
            out.println(cli.getHelpMessage());
            out.println(USAGE);
            return;
        }
        if (options.getVersion() || options.getCite()) {
            System.out.println(VERSION);
            return;
        }
        if (args.length == 0) {
            System.out.println(VERSION);
            System.out.println(USAGE);
            System.out.println("write 'decomp --help' for further information");
            return;
        }
        final String filter = options.getFilter().toUpperCase();
        final FilterLevel level = FilterLevel.valueOf(filter);
        DecompositionValidator<Element> validator = null;
        if (level == null) {
            System.err.println("Unknown filter '" + options.getFilter() + "'. Allowed are strict, common, permissive, rdbe, none.\n");
            System.err.println(cli.getHelpMessage());
            System.exit(1);
        } else if (level != FilterLevel.NONE && options.getDontUseRDBE()) {
            System.err.println("Conflicting options: --nofilter and --filter='" + options.getFilter() + "'. Only one of both must be set.\n");
            System.err.println(cli.getHelpMessage());
            System.exit(1);
        } else {
            switch (level) {
                case STRICT:
                    validator = ChemicalValidator.getStrictThreshold();
                    break;
                case COMMON:
                    validator = ChemicalValidator.getCommonThreshold();
                    break;
                case PERMISSIVE:
                    validator = ChemicalValidator.getPermissiveThreshold();
                    break;
                case RDBE:
                    validator = new ValenceValidator<Element>();
                    break;
                case NONE:
                    validator = null;
            }
        }
        final double mass;
        final double mz = options.getMass();
        final String ion = options.getIonization();
        final PrecursorIonType ionization = ion == null ? PeriodicTable.getInstance().ionByName("[M+H]+") : PeriodicTable.getInstance().ionByName(ion);
        if (ionization == null) {
            System.err.println("Unknown ion '" + ion + "'");
            return;
        }
        mass = ionization.precursorMassToNeutralMass(mz);
        if (validator == null) {
            // do nothing
        } else if (validator instanceof ChemicalValidator) {
            final ChemicalValidator c = (ChemicalValidator) validator;
            validator = new ChemicalValidator(c.getRdbeThreshold(), c.getRdbeLowerbound() + 0.5d, c.getHeteroToCarbonThreshold(), c.getHydrogenToCarbonThreshold());
        } else if (validator instanceof ValenceValidator) {
            validator = new ValenceValidator<Element>(0d);
        }
        final Deviation dev = new Deviation(options.getPPM(), options.getAbsoluteDeviation());
        final ChemicalAlphabet alphabet = options.getAlphabet().getAlphabet();
        final MassToFormulaDecomposer decomposer = new MassToFormulaDecomposer(alphabet);

        Map<Element, Interval> boundary = options.getAlphabet().getBoundary();
        final String parentFormula = options.getParentFormula();
        if (parentFormula != null) {
            final MolecularFormula formula = MolecularFormula.parse(parentFormula);
            for (Element e : alphabet.getElements()) {
                Interval i = boundary.get(e);
                if (i == null) {
                    i = new Interval(0, formula.numberOf(e));
                    boundary.put(e, i);
                } else {
                    boundary.put(e, new Interval(i.getMin(), Math.min(i.getMax(), formula.numberOf(e))));
                }
            }
        }
        /*
        if (validator != null && (validator instanceof ValenceValidator)) {
            boundary = new ValenceBoundary<Element>(options.getAlphabet().getAlphabet()).getMapFor(mass +
                    dev.absoluteFor(mass), options.getAlphabet().getBoundary());
        }
        */

        final List<int[]> compomers = decomposer.decompose(mass, dev, boundary, validator);
        final List<MolecularFormula> formulas = new ArrayList<MolecularFormula>(compomers.size());
        for (int[] c : compomers) {
            formulas.add(alphabet.decompositionToFormula(c));
        }
        Collections.sort(formulas, new Comparator<MolecularFormula>() {
            @Override
            public int compare(MolecularFormula o1, MolecularFormula o2) {
                return Double.compare(Math.abs(o1.getMass() - mass), Math.abs(o2.getMass() - mass));
            }
        });
        final boolean printErrors = options.getMassErrors();
        final DecimalFormat formater = (DecimalFormat) NumberFormat.getInstance(Locale.ENGLISH);
        formater.applyPattern("#.##");
        for (MolecularFormula f : formulas) {
            System.out.print(f);
            if (printErrors) {
                System.out.print("\t");
                System.out.print(mass - f.getMass());
                System.out.print("\t");
                System.out.println(formater.format(((mass - f.getMass()) / mass) * 1e6));
            } else {
                System.out.println("");
            }
        }
    }

}
