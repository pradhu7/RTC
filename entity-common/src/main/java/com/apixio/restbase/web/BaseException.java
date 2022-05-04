package com.apixio.restbase.web;

import java.util.HashMap;
import java.util.Map;

import com.apixio.utility.StringUtil;

/**
 * This class is meant to be both a way to capture generically the detailed
 * reasons for a business-logic-level exception (to be able to pass back those
 * reasons in a JSON object) and a base class for a concrete exception class
 * that can be used by Java code so the failure reason can be dealt with
 * immediately in that Java code.
 *
 * The hoped-for pattern is for the business logic to extend this class when
 * declaring its main Exception class.  The constructor of those Exception classes
 * then should call "super(failureType)" where failureType is the normal Java
 * enum that is the standard business logic error model.
 */
public class BaseException extends RuntimeException {

    /**
     * The field names of the JSON object returned when a bad request was submitted by the
     * client.  The field value of BAD_PARAM_NAME will contain the (hopefully documented)
     * name of the parameter and the field value of BAD_PARAM_VALUE will contain the bad
     * value (if applicable).
     */
    public final static String BAD_PARAM_NAME  = "invalid_param_name";
    public final static String BAD_PARAM_VALUE = "invalid_param_value";

    /**
     * This is the JSON field name of the JSON object returned for an error.  The
     * value of the field will be either the Java enum name or the value returned
     * from BaseFailure.getReason().  This is a prgrammatic reason and is not meant
     * to be useful to humans.
     */
    public final static String P_REASON = "reason";

    /**
     * This is the JSON field name of the JSON object returned for an error; this
     * is meant to be useful/descriptive to humans.
     */
    public final static String P_DESCRIPTION = "description";

    /**
     * Bad requests are handled specially as they're general in nature and because
     * multiple levels of code can detect such problems.
     */
    private boolean isBadRequest;

    /**
     * Entity was not found.
     */
    private boolean isNotFound;

    /**
     * The enum-based failure.  All we can do with this is to use the String form of
     * the enum name as we have no type information.
     */
    protected Enum failureType;

    /**
     * Keeps track of whether or not we've already added the reason to the details Map.
     * We can't add the reason in the constructor as we'd have to make a call to the
     * child class before 'this' is fully instantiated.
     */
    private boolean gotReason;

    /**
     * The details map holds the field name=value set that describes the error
     * in detail.
     */
    private Map<String, String> details = new HashMap<String, String>();

    /**
     * Construct an Exception whose failure reason is given via getReason().
     */
    protected BaseException()
    {
    }

    /**
     * Not-found:  resource identified by URL was not found.  This is reserved for when
     * the entity identified by an element of the URL didn't refer to a known entity.
     */
    public static BaseException notFound()
    {
        BaseException bx = new BaseException();

        bx.isNotFound = true;

        return bx;
    }

    /**
     * Generic bad request... no details.  BaseRS handles this with a simple SC_BAD_REQUEST
     */
    public static BaseException badRequest()
    {
        BaseException bx = new BaseException();

        bx.isBadRequest = true;

        return bx;
    }

    /**
     * ??? temporary?  used by customer *logic code...
     */
    public static BaseException badRequest(String format, Object... params)
    {
        BaseException bx = new BaseException();

        bx.isBadRequest = true;
        if (format != null)
            bx.description(format, params);

        return bx;
    }

    /**
     * A bad request specifically because a bad/missing parameter was found.  BaseRS handles this
     * with SC_BAD_REQUEST and adds details from here.
     */
    public static BaseException badRequestParam(String name, Object value, String humanReadableFormat)
    {
        BaseException bx = new BaseException();

        bx.isBadRequest = true;

        if (humanReadableFormat != null)
            bx.description(humanReadableFormat, value);

        if (name != null)
            bx.detail(BAD_PARAM_NAME, name);

        if (value != null)
            bx.detail(BAD_PARAM_VALUE, value.toString());

        return bx;
    }

    /**
     * Construct an Exception whose failure is the simple name of the given
     * enumeration, assumed here to be from the FailureType enum of the
     * various business logic classes.
     */
    public BaseException(Enum failure)
    {
        failureType = failure;
    }

    /**
     * Returns true if the BaseException was created via badRequest() or badRequestParam().
     */
    public boolean isBadRequest()
    {
        return isBadRequest;
    }

    /**
     * Returns true if the BaseException was created via notFound()
     */
    public boolean isNotFound()
    {
        return isNotFound;
    }

    /**
     * Add a new name=value field to the JSON object to be returned.  The name=value
     * is just supposed to give more detailed information to the RESTful API client.
     * The fields added via this method are intended to be programmatic in nature
     * vs descriptive (for humans).
     */
    public BaseException detail(Map<String, String> details)
    {
        if (details != null)
        {
            if (this.details.size() == 0)
                this.details = details;
            else
                this.details.putAll(details);
        }

        return this;
    }
    public BaseException detail(String name, String value)
    {
        details.put(name, value);

        return this;
    }

    /**
     * Add a human-readable reason/description of the error/failure.
     */
    public BaseException description(String format, Object... args)
    {
        details.put(P_DESCRIPTION, StringUtil.subargsPos(format, args));

        return this;
    }

    /**
     * Get the default (enum-name-based) reason for the failure.
     */
    protected String getReason()
    {
        if (failureType != null)
            return failureType.toString();
        else
            return null;
    }

    /**
     * Return the details of the failure
     */
    public Map<String, String> getDetails()
    {
        if (!gotReason)
        {
            String reason = getReason();

            if (reason != null)
                details.put(P_REASON, reason);

            gotReason = true;
        }

        return details;
    }

    /**
     * Returns the human-readable description of the failure.
     */
    public String getDescription()
    {
        return details.get(P_DESCRIPTION);
    }
}
