package apixio.infraconfig.core;

import com.nulabinc.zxcvbn.Strength;
import com.nulabinc.zxcvbn.Zxcvbn;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import org.passay.CharacterData;
import org.passay.CharacterRule;
import org.passay.EnglishCharacterData;
import org.passay.EnglishSequenceData;
import org.passay.IllegalSequenceRule;
import org.passay.LengthRule;
import org.passay.PasswordData;
import org.passay.PasswordGenerator;
import org.passay.PasswordValidator;
import org.passay.RuleResult;
import org.passay.WhitespaceRule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class ValidatePassword {
    protected Zxcvbn zxcvbn;
    private List<String> extraDictionary;
    private Integer minPasswordStrength = 3;
    protected PasswordValidator passayValidator;
    private List<CharacterRule> passayGeneratorRules;
    private PasswordGenerator passayGenerator;
    private final CharacterData specialChars = new CharacterData() {
        @Override
        public String getErrorCode() {
            return "INSUFFICIENT_SPECIAL";
        }

        @Override
        public String getCharacters() {
            return "#$%&()*+,-./:;<=>?@[]^_`{|}~";
        }
    };

    private Integer minLength = 8;
    private Integer maxLength = 32;
    private Integer minUpperChars = 1;
    private Integer minLowerChars = 1;
    private Integer minDigits = 1;
    private Integer minSpecial = 1;
    private Integer maxSequence = 4;

    public ValidatePassword() {
        this(new String[0]);
    }

    public ValidatePassword(String... extraDictionary) {
        this.zxcvbn = new Zxcvbn();
        this.extraDictionary = new ArrayList<>();
        addDictEntry(extraDictionary);
        addDictEntry("apixio", "sftp");
        createPassayValidator();
        createPassayGenerator();
    }

    private void createPassayValidator() {
        passayValidator = new PasswordValidator(
                // length between 8 and 16 characters
                new LengthRule(minLength, maxLength),

                // at least one upper-case character
                new CharacterRule(EnglishCharacterData.UpperCase, minUpperChars),

                // at least one lower-case character
                new CharacterRule(EnglishCharacterData.LowerCase, minLowerChars),

                // at least one digit character
                new CharacterRule(EnglishCharacterData.Digit, minDigits),

                // at least one symbol (special character)
                new CharacterRule(specialChars, minSpecial),

                // define some illegal sequences that will fail when >= 5 chars long
                // alphabetical is of the form 'abcde', numerical is '34567', qwery is 'asdfg'
                // the false parameter indicates that wrapped sequences are allowed; e.g. 'xyzabc'
                new IllegalSequenceRule(EnglishSequenceData.Alphabetical, maxSequence, false),
                new IllegalSequenceRule(EnglishSequenceData.Numerical, maxSequence, false),
                new IllegalSequenceRule(EnglishSequenceData.USQwerty, maxSequence, false),

                // no whitespace
                new WhitespaceRule()
        );
    }

    private void createPassayGenerator() {
        passayGeneratorRules = Arrays.asList(
                // at least one upper-case character
                new CharacterRule(EnglishCharacterData.UpperCase, minUpperChars),

                // at least one lower-case character
                new CharacterRule(EnglishCharacterData.LowerCase, minLowerChars),

                // at least one digit character
                new CharacterRule(EnglishCharacterData.Digit, minDigits),

                // at least one speical charater
                new CharacterRule(specialChars, minSpecial)
        );

        passayGenerator = new PasswordGenerator();
    }

    public void addDictEntry(String... extraDictEntries) {
        extraDictionary.addAll(Arrays.asList(extraDictEntries));
    }

    public void setMinimumStrength(Integer strength) {
        minPasswordStrength = strength;
    }

    public void setMinLength(Integer minLength) {
        this.minLength = minLength;
        createPassayValidator();
    }

    public void setMaxLength(Integer maxLength) {
        this.maxLength = maxLength;
        createPassayValidator();
    }

    public void setMinUpperChars(Integer minUpperChars) {
        this.minUpperChars = minUpperChars;
        createPassayValidator();
        createPassayGenerator();
    }

    public void setMinLowerChars(Integer minLowerChars) {
        this.minLowerChars = minLowerChars;
        createPassayValidator();
        createPassayGenerator();
    }

    public void setMinDigits(Integer minDigits) {
        this.minDigits = minDigits;
        createPassayValidator();
        createPassayGenerator();
    }

    public void setMinSpecial(Integer minSpecial) {
        this.minSpecial = minSpecial;
        createPassayValidator();
        createPassayGenerator();
    }

    public void setMaxSequence(Integer maxSequence) {
        this.maxSequence = maxSequence;
        createPassayValidator();
    }

    public Triple<Boolean, String, String> checkPassword(String password) {
        return checkPassword(null, password);
    }

    public Triple<Boolean, String, String> checkPassword(String username, String password) {
        if (Objects.nonNull(username)) {
            addDictEntry(username);
        }
        Strength strength = zxcvbn.measure(password, extraDictionary);
        RuleResult passayResult = passayValidator.validate(new PasswordData(password));
        Boolean checkSuccess = (strength.getScore() >= minPasswordStrength) && passayResult.isValid();

        List<String> feedbackList = new ArrayList<>();
        feedbackList.add(strength.getFeedback().getWarning());
        feedbackList.addAll(passayValidator.getMessages(passayResult));

        List<String> suggestionList = new ArrayList<>();
        suggestionList.addAll(strength.getFeedback().getSuggestions());

        return new ImmutableTriple<>(checkSuccess, " - " + String.join("\n - ", feedbackList), " - " + String.join("\n - ", suggestionList));
    }

    public String generatePassword(Integer length) {
        return passayGenerator.generatePassword(length, passayGeneratorRules);
    }
}
