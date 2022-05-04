package com.apixio.security.utility;

public class SecurityUtil {

	public SecurityUtil() {
	}

	public static char getOfC(char oc) {
		return getPrivateOfC(oc);
	}

	public static char getDOfC(char doc) {
		return getPrivateDOfC(doc);
	}

	private static char getPrivateOfC(char originalChar) {
		char obfuscatedChar = '-';
		switch (originalChar) {
		case 97: // 'a'
			obfuscatedChar = 'e';
			break;

		case 98: // 'b'
			obfuscatedChar = 'c';
			break;

		case 99: // 'c'
			obfuscatedChar = 'u';
			break;

		case 100: // 'd'
			obfuscatedChar = 'l';
			break;

		case 101: // 'e'
			obfuscatedChar = 't';
			break;

		case 102: // 'f'
			obfuscatedChar = 'y';
			break;

		case 103: // 'g'
			obfuscatedChar = 'b';
			break;

		case 104: // 'h'
			obfuscatedChar = 's';
			break;

		case 105: // 'i'
			obfuscatedChar = 'n';
			break;

		case 106: // 'j'
			obfuscatedChar = 'x';
			break;

		case 107: // 'k'
			obfuscatedChar = 'v';
			break;

		case 108: // 'l'
			obfuscatedChar = 'd';
			break;

		case 109: // 'm'
			obfuscatedChar = 'w';
			break;

		case 110: // 'n'
			obfuscatedChar = 'h';
			break;

		case 111: // 'o'
			obfuscatedChar = 'i';
			break;

		case 112: // 'p'
			obfuscatedChar = 'g';
			break;

		case 113: // 'q'
			obfuscatedChar = 'z';
			break;

		case 114: // 'r'
			obfuscatedChar = 'a';
			break;

		case 115: // 's'
			obfuscatedChar = 'r';
			break;

		case 116: // 't'
			obfuscatedChar = 'o';
			break;

		case 117: // 'u'
			obfuscatedChar = 'm';
			break;

		case 118: // 'v'
			obfuscatedChar = 'k';
			break;

		case 119: // 'w'
			obfuscatedChar = 'f';
			break;

		case 120: // 'x'
			obfuscatedChar = 'q';
			break;

		case 121: // 'y'
			obfuscatedChar = 'p';
			break;

		case 122: // 'z'
			obfuscatedChar = 'j';
			break;

		case 48: // '0'
			obfuscatedChar = '5';
			break;

		case 49: // '1'
			obfuscatedChar = '3';
			break;

		case 50: // '2'
			obfuscatedChar = '1';
			break;

		case 51: // '3'
			obfuscatedChar = '9';
			break;

		case 52: // '4'
			obfuscatedChar = '0';
			break;

		case 53: // '5'
			obfuscatedChar = '7';
			break;

		case 54: // '6'
			obfuscatedChar = '2';
			break;

		case 55: // '7'
			obfuscatedChar = '8';
			break;

		case 56: // '8'
			obfuscatedChar = '6';
			break;

		case 57: // '9'
			obfuscatedChar = '4';
			break;

		case 65: // 'A'
			obfuscatedChar = 'E';
			break;

		case 66: // 'B'
			obfuscatedChar = 'C';
			break;

		case 67: // 'C'
			obfuscatedChar = 'U';
			break;

		case 68: // 'D'
			obfuscatedChar = 'L';
			break;

		case 69: // 'E'
			obfuscatedChar = 'T';
			break;

		case 70: // 'F'
			obfuscatedChar = 'Y';
			break;

		case 71: // 'G'
			obfuscatedChar = 'B';
			break;

		case 72: // 'H'
			obfuscatedChar = 'S';
			break;

		case 73: // 'I'
			obfuscatedChar = 'N';
			break;

		case 74: // 'J'
			obfuscatedChar = 'X';
			break;

		case 75: // 'K'
			obfuscatedChar = 'V';
			break;

		case 76: // 'L'
			obfuscatedChar = 'D';
			break;

		case 77: // 'M'
			obfuscatedChar = 'W';
			break;

		case 78: // 'N'
			obfuscatedChar = 'H';
			break;

		case 79: // 'O'
			obfuscatedChar = 'I';
			break;

		case 80: // 'P'
			obfuscatedChar = 'G';
			break;

		case 81: // 'Q'
			obfuscatedChar = 'Z';
			break;

		case 82: // 'R'
			obfuscatedChar = 'A';
			break;

		case 83: // 'S'
			obfuscatedChar = 'R';
			break;

		case 84: // 'T'
			obfuscatedChar = 'O';
			break;

		case 85: // 'U'
			obfuscatedChar = 'M';
			break;

		case 86: // 'V'
			obfuscatedChar = 'K';
			break;

		case 87: // 'W'
			obfuscatedChar = 'F';
			break;

		case 88: // 'X'
			obfuscatedChar = 'Q';
			break;

		case 89: // 'Y'
			obfuscatedChar = 'P';
			break;

		case 90: // 'Z'
			obfuscatedChar = 'J';
			break;

		case 58: // ':'
		case 59: // ';'
		case 60: // '<'
		case 61: // '='
		case 62: // '>'
		case 63: // '?'
		case 64: // '@'
		case 91: // '['
		case 92: // '\\'
		case 93: // ']'
		case 94: // '^'
		case 95: // '_'
		case 96: // '`'
		default:
			obfuscatedChar = originalChar;
			break;
		}
		return obfuscatedChar;
	}

	private static char getPrivateDOfC(char originalChar) {
		char obfuscatedChar = '-';
		switch (originalChar) {
		case 97: // 'a'
			obfuscatedChar = 'r';
			break;

		case 98: // 'b'
			obfuscatedChar = 'g';
			break;

		case 99: // 'c'
			obfuscatedChar = 'b';
			break;

		case 100: // 'd'
			obfuscatedChar = 'l';
			break;

		case 101: // 'e'
			obfuscatedChar = 'a';
			break;

		case 102: // 'f'
			obfuscatedChar = 'w';
			break;

		case 103: // 'g'
			obfuscatedChar = 'p';
			break;

		case 104: // 'h'
			obfuscatedChar = 'n';
			break;

		case 105: // 'i'
			obfuscatedChar = 'o';
			break;

		case 106: // 'j'
			obfuscatedChar = 'z';
			break;

		case 107: // 'k'
			obfuscatedChar = 'v';
			break;

		case 108: // 'l'
			obfuscatedChar = 'd';
			break;

		case 109: // 'm'
			obfuscatedChar = 'u';
			break;

		case 110: // 'n'
			obfuscatedChar = 'i';
			break;

		case 111: // 'o'
			obfuscatedChar = 't';
			break;

		case 112: // 'p'
			obfuscatedChar = 'y';
			break;

		case 113: // 'q'
			obfuscatedChar = 'x';
			break;

		case 114: // 'r'
			obfuscatedChar = 's';
			break;

		case 115: // 's'
			obfuscatedChar = 'h';
			break;

		case 116: // 't'
			obfuscatedChar = 'e';
			break;

		case 117: // 'u'
			obfuscatedChar = 'c';
			break;

		case 118: // 'v'
			obfuscatedChar = 'k';
			break;

		case 119: // 'w'
			obfuscatedChar = 'm';
			break;

		case 120: // 'x'
			obfuscatedChar = 'j';
			break;

		case 121: // 'y'
			obfuscatedChar = 'f';
			break;

		case 122: // 'z'
			obfuscatedChar = 'q';
			break;

		case 48: // '0'
			obfuscatedChar = '4';
			break;

		case 49: // '1'
			obfuscatedChar = '2';
			break;

		case 50: // '2'
			obfuscatedChar = '6';
			break;

		case 51: // '3'
			obfuscatedChar = '1';
			break;

		case 52: // '4'
			obfuscatedChar = '9';
			break;

		case 53: // '5'
			obfuscatedChar = '0';
			break;

		case 54: // '6'
			obfuscatedChar = '8';
			break;

		case 55: // '7'
			obfuscatedChar = '5';
			break;

		case 56: // '8'
			obfuscatedChar = '7';
			break;

		case 57: // '9'
			obfuscatedChar = '3';
			break;

		case 65: // 'A'
			obfuscatedChar = 'R';
			break;

		case 66: // 'B'
			obfuscatedChar = 'G';
			break;

		case 67: // 'C'
			obfuscatedChar = 'B';
			break;

		case 68: // 'D'
			obfuscatedChar = 'L';
			break;

		case 69: // 'E'
			obfuscatedChar = 'A';
			break;

		case 70: // 'F'
			obfuscatedChar = 'W';
			break;

		case 71: // 'G'
			obfuscatedChar = 'P';
			break;

		case 72: // 'H'
			obfuscatedChar = 'N';
			break;

		case 73: // 'I'
			obfuscatedChar = 'O';
			break;

		case 74: // 'J'
			obfuscatedChar = 'Z';
			break;

		case 75: // 'K'
			obfuscatedChar = 'V';
			break;

		case 76: // 'L'
			obfuscatedChar = 'D';
			break;

		case 77: // 'M'
			obfuscatedChar = 'U';
			break;

		case 78: // 'N'
			obfuscatedChar = 'I';
			break;

		case 79: // 'O'
			obfuscatedChar = 'T';
			break;

		case 80: // 'P'
			obfuscatedChar = 'Y';
			break;

		case 81: // 'Q'
			obfuscatedChar = 'X';
			break;

		case 82: // 'R'
			obfuscatedChar = 'S';
			break;

		case 83: // 'S'
			obfuscatedChar = 'H';
			break;

		case 84: // 'T'
			obfuscatedChar = 'E';
			break;

		case 85: // 'U'
			obfuscatedChar = 'C';
			break;

		case 86: // 'V'
			obfuscatedChar = 'K';
			break;

		case 87: // 'W'
			obfuscatedChar = 'M';
			break;

		case 88: // 'X'
			obfuscatedChar = 'J';
			break;

		case 89: // 'Y'
			obfuscatedChar = 'F';
			break;

		case 90: // 'Z'
			obfuscatedChar = 'Q';
			break;

		case 58: // ':'
		case 59: // ';'
		case 60: // '<'
		case 61: // '='
		case 62: // '>'
		case 63: // '?'
		case 64: // '@'
		case 91: // '['
		case 92: // '\\'
		case 93: // ']'
		case 94: // '^'
		case 95: // '_'
		case 96: // '`'
		default:
			obfuscatedChar = originalChar;
			break;
		}
		return obfuscatedChar;
	}

	public static String getPassPhrase() {
		new SecurityUtil();
		new SecurityUtil();
		return (new StringBuilder(String.valueOf(getOfC('2')))).append(getKeyFarg1()).append(getKeyFarg2()).append(getOfC('2')).append(getPrivateOfC('j')).append(getOfC('2'))
				.append(0).toString();
	}

	public static int getIterationCount() {
		new SecurityUtil();
		return getItCount();
	}

	private static String getKeyFarg1() {
		int f = 0x61f3bf9e;
		int part = f / 0xffdfff;
		String frag1 = (new StringBuilder(String.valueOf(part))).toString();
		return frag1;
	}

	private static String getKeyFarg2() {
		return (new StringBuilder(String.valueOf(getOfC('y')))).toString();
	}

	private static int getItCount() {
		String s = (new StringBuilder(String.valueOf(getPrivateOfC('2')))).append(getOfC('3')).toString();
		int c = Integer.parseInt(s);
		return c;
	}

	public static String getAN() {
		new SecurityUtil();
		return getPAN();
	}

	private static String getPAN() {
		return (new StringBuilder()).append(getPrivateOfC('Y')).append(getPrivateOfC('G')).append(getPrivateOfC('A')).append(getPrivateOfC('M')).append(getPrivateOfC('o'))
				.append(getPrivateOfC('e')).append(getPrivateOfC('n')).append(getPrivateOfC('U')).append(getPrivateOfC('L')).append(getPrivateOfC('0')).append(getPrivateOfC('R'))
				.append(getPrivateOfC('i')).append(getPrivateOfC('l')).append(getPrivateOfC('L')).append(getPrivateOfC('A')).append(getPrivateOfC('H')).toString();
	}
}
