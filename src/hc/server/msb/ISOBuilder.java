package hc.server.msb;

import java.util.HashMap;
import java.util.Iterator;

/**
 * 停止使用
 */
public class ISOBuilder {
	private static HashMap<String, Integer> map3166_1 = buildMap3166_1();
	private static HashMap<Integer, String> reverse_map3166_1 = buildReverseMap3166_1(map3166_1);

	private static HashMap<Integer, String> buildReverseMap3166_1(HashMap<String, Integer> map) {
		Iterator<String> it = map.keySet().iterator();

		HashMap<Integer, String> outmap = new HashMap<Integer, String>(map.size());

		while (it.hasNext()) {
			String c = it.next();
			outmap.put(map.get(c), c);
		}

		return outmap;
	}

	private static HashMap<String, Integer> buildMap3166_1() {
		HashMap<String, Integer> map = new HashMap<String, Integer>();

		map.put("Andorra", 20);
		map.put("United Arab Emirates", 784);
		map.put("Afghanistan", 4);
		map.put("Antigua Barbuda", 28);
		map.put("Anguilla", 660);
		map.put("Albania", 8);
		map.put("Armenia", 51);
		map.put("Angola", 24);
		map.put("Antarctica", 10);
		map.put("Argentina", 32);
		map.put("American Samoa", 16);
		map.put("Austria", 40);
		map.put("Australia", 36);
		map.put("Aruba", 533);
		map.put("Åaland Island", 248);
		map.put("Azerbaijan", 31);
		map.put("Bosnia Herzegovina", 70);
		map.put("Barbados", 52);
		map.put("Bangladesh", 50);
		map.put("Belgium", 56);
		map.put("Burkina", 854);
		map.put("Bulgaria", 100);
		map.put("Bahrain", 48);
		map.put("Burundi", 108);
		map.put("Benin", 204);
		map.put("Saint Barthélemy", 652);
		map.put("Bermuda", 60);
		map.put("Brunei", 96);
		map.put("Bolivia", 68);
		map.put("Caribbean Netherlands", 535);
		map.put("Brazil", 76);
		map.put("The Bahamas", 44);
		map.put("Bhutan", 64);
		map.put("Bouvet Island", 74);
		map.put("Botswana", 72);
		map.put("Belarus", 112);
		map.put("Belize", 84);
		map.put("Canada", 124);
		map.put("Cocos (Keeling) Islands", 166);
		map.put("Democratic Republic of the Congo", 180);
		map.put("Central African Republic", 140);
		map.put("Republic of the Congo", 178);
		map.put("Switzerland", 756);
		map.put("Côte d'ivoire", 384);
		map.put("Cook Islands", 184);
		map.put("Chile", 152);
		map.put("Cameroon", 120);
		map.put("China", 156);
		map.put("Colombia", 170);
		map.put("Costa Rica", 188);
		map.put("Cuba", 192);
		map.put("Cape Verde", 132);
		map.put("Christmas Island", 162);
		map.put("Cyprus", 196);
		map.put("Czech Republic", 203);
		map.put("Germany", 276);
		map.put("Djibouti", 262);
		map.put("Denmark", 208);
		map.put("Dominica", 212);
		map.put("Dominican Republic", 214);
		map.put("Algeria", 12);
		map.put("Ecuado", 218);
		map.put("Estonia", 233);
		map.put("Egypt", 818);
		map.put("Western Sahara", 732);
		map.put("Eritrea", 232);
		map.put("Spain", 724);
		map.put("Ethiopia", 231);
		map.put("Finland", 246);
		map.put("Fiji", 242);
		map.put("Falkland Islands", 238);
		map.put("Federated States of Micronesia", 583);
		map.put("Faroe Islands", 234);
		map.put("France", 250);
		map.put("Gabon", 266);
		map.put("Great Britain (United Kingdom; England)", 826);
		map.put("Grenada", 308);
		map.put("Georgia", 268);
		map.put("French Guiana", 254);
		map.put("Guernsey", 831);
		map.put("Ghana", 288);
		map.put("Gibraltar", 292);
		map.put("Greenland", 304);
		map.put("Gambia", 270);
		map.put("Guinea", 324);
		map.put("Guadeloupe", 312);
		map.put("Equatorial Guinea", 226);
		map.put("Greece", 300);
		map.put("South Georgia and the South Sandwich Islands", 239);
		map.put("Guatemala", 320);
		map.put("Guam", 316);
		map.put("Guinea-Bissau", 624);
		map.put("Guyana", 328);
		map.put("Hong Kong", 344);
		map.put("Heard Island and McDonald Islands", 334);
		map.put("Honduras", 340);
		map.put("Croatia", 191);
		map.put("Haiti", 332);
		map.put("Hungary", 348);
		map.put("Indonesia", 360);
		map.put("Ireland", 372);
		map.put("Israel", 376);
		map.put("Isle of Man", 833);
		map.put("India", 356);
		map.put("British Indian Ocean Territory", 86);
		map.put("Iraq", 368);
		map.put("Iran", 364);
		map.put("Iceland", 352);
		map.put("Italy", 380);
		map.put("Jersey", 832);
		map.put("Jamaica", 388);
		map.put("Jordan", 400);
		map.put("Japan", 392);
		map.put("Kenya", 404);
		map.put("Kyrgyzstan", 417);
		map.put("Cambodia", 116);
		map.put("Kiribati", 296);
		map.put("The Comoros", 174);
		map.put("St. Kitts Nevis", 659);
		map.put("North Korea", 408);
		map.put("South Korea", 410);
		map.put("Kuwait", 414);
		map.put("Cayman Islands", 136);
		map.put("Kazakhstan", 398);
		map.put("Laos", 418);
		map.put("Lebanon", 422);
		map.put("St. Lucia", 662);
		map.put("Liechtenstein", 438);
		map.put("Sri Lanka", 144);
		map.put("Liberia", 430);
		map.put("Lesotho", 426);
		map.put("Lithuania", 440);
		map.put("Luxembourg", 442);
		map.put("Latvia", 428);
		map.put("Libya", 434);
		map.put("Morocco", 504);
		map.put("Monaco", 492);
		map.put("Moldova", 498);
		map.put("Montenegro", 499);
		map.put("Saint Martin (France)", 663);
		map.put("Madagascar", 450);
		map.put("Marshall islands", 584);
		map.put("Republic of Macedonia", 807);
		map.put("Mali", 466);
		map.put("Myanmar (Burma)", 104);
		map.put("Mongolia", 496);
		map.put("Macao", 446);
		map.put("Northern Mariana Islands", 580);
		map.put("Martinique", 474);
		map.put("Mauritania", 478);
		map.put("Montserrat", 500);
		map.put("Malta", 470);
		map.put("Mauritius", 480);
		map.put("Maldives", 462);
		map.put("Malawi", 454);
		map.put("Mexico", 484);
		map.put("Malaysia", 458);
		map.put("Mozambique", 508);
		map.put("Namibia", 516);
		map.put("New Caledonia", 540);
		map.put("Niger", 562);
		map.put("Norfolk Island", 574);
		map.put("Nigeria", 566);
		map.put("Nicaragua", 558);
		map.put("Netherlands", 528);
		map.put("Norway", 578);
		map.put("Nepal", 524);
		map.put("Nauru", 520);
		map.put("Niue", 570);
		map.put("New Zealand", 554);
		map.put("Oman", 512);
		map.put("Panama", 591);
		map.put("Peru", 604);
		map.put("French polynesia", 258);
		map.put("Papua New Guinea", 598);
		map.put("The Philippines", 608);
		map.put("Pakistan", 586);
		map.put("Poland", 616);
		map.put("Saint-Pierre and Miquelon", 666);
		map.put("Pitcairn Islands", 612);
		map.put("Puerto Rico", 630);
		map.put("Palestinian territories", 275);
		map.put("Portugal", 620);
		map.put("Palau", 585);
		map.put("Paraguay", 600);
		map.put("Qatar", 634);
		map.put("Réunion", 638);
		map.put("Romania", 642);
		map.put("Serbia", 688);
		map.put("Russian Federation", 643);
		map.put("Rwanda", 646);
		map.put("Saudi Arabia", 682);
		map.put("Solomon Islands", 90);
		map.put("Seychelles", 690);
		map.put("Sudan", 729);
		map.put("Sweden", 752);
		map.put("Singapore", 702);
		map.put("St. Helena Dependencies", 654);
		map.put("Slovenia", 705);
		map.put("Svalbard and Jan Mayen", 744);
		map.put("Slovakia", 703);
		map.put("Sierra Leone", 694);
		map.put("San Marino", 674);
		map.put("Senegal", 686);
		map.put("Somalia", 706);
		map.put("Suriname", 740);
		map.put("South Sudan", 728);
		map.put("Sao Tome Principe", 678);
		map.put("El Salvador", 222);
		map.put("Syria", 760);
		map.put("Swaziland", 748);
		map.put("Turks Caicos Islands", 796);
		map.put("Chad", 148);
		map.put("French Southern Territories", 260);
		map.put("Togo", 768);
		map.put("Thailand", 764);
		map.put("Tajikistan", 762);
		map.put("Tokelau", 772);
		map.put("Timor-Leste(East Timor)", 626);
		map.put("Turkmenistan", 795);
		map.put("Tunisia", 788);
		map.put("Tonga", 776);
		map.put("Turkey", 792);
		map.put("Trinidad Tobago", 780);
		map.put("Tuvalu", 798);
		map.put("Taiwan, Province of China", 158);
		map.put("Tanzania", 834);
		map.put("Ukraine", 804);
		map.put("Uganda", 800);
		map.put("United States Minor Outlying Islands", 581);
		map.put("United States of America(USA)", 840);
		map.put("Uruguay", 858);
		map.put("Uzbekistan", 860);
		map.put("Vatican City(The Holy See)", 336);
		map.put("St. Vincent the Grenadines", 670);
		map.put("Venezuela", 862);
		map.put("British Virgin Islands", 92);
		map.put("United States Virgin Islands", 850);
		map.put("Vietnam", 704);
		map.put("Vanuatu", 548);
		map.put("Wallis and Futuna", 876);
		map.put("Samoa", 882);
		map.put("Yemen", 887);
		map.put("Mayotte", 175);
		map.put("South Africa", 710);
		map.put("Zambia", 894);
		map.put("Zimbabwe", 716);

		return map;
	}

	public static String getDevCountryDesc(final int dev_country) {
		String c = reverse_map3166_1.get(dev_country);
		if (c != null) {
			return c;
		}
		return "";
	}

	public static String getDevTypeDesc(final int dev_hscode) {
		return "";
	}

	/**
	 * www.foreign-trade.com/reference/hscode.cfm?cat=13
	 * 
	 * @return
	 */
	private static HashMap<Integer, String> buildHSCode() {
		HashMap<Integer, String> map = new HashMap<Integer, String>();

		map.put(8501, "electric motors and generators");

		return map;
	}
}
