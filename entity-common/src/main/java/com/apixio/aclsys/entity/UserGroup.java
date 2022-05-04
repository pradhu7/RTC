package com.apixio.aclsys.entity;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.apixio.XUUID;

/**
 * UserGroup is a JSON-serializable POJO that represents a group of users
 * stored within Cassandra.
 *
 * This POJO does *not* record any group membership--it's simply a Java
 * representation of the metadata of a UserGroup.
 */
public class UserGroup {

    // for tagging the XUUID
    public final static String OBJTYPE = "G";

    /**
     * The names of all system groups have this prefix.  The correct use of this
     * is left to the clients (kind of a poor model, but affects only code right now).
     */
    public final static String SYSTYPE_PREFIX = "System:";

    private final static String F_ID        = "id";
    private final static String F_NAME      = "name";
    private final static String F_CANONICAL = "canon";
    private final static String F_TYPE      = "type";   // type of UserGroup:  [null (nothing special), "System:Role"]

    private static ObjectMapper objectMapper = new ObjectMapper();


    private XUUID id;

    /**
     * As-entered (but cleaned).
     */
    private String groupName;

    /**
     * Canonicalized; guaranteed to be unique within system.
     */
    private String canonicalName;

    /**
     * Type of group.  Can be null, which means it is not a special (system) group.
     */
    private String groupType;

    public UserGroup(XUUID id, String cleanName, String canonName)
    {
        this.id            = id;
        this.groupName     = cleanName;
        this.canonicalName = canonName;
    }

    public static UserGroup fromJson(String json)
    {
        try
        {
            JsonNode  root = objectMapper.readTree(json);
            JsonNode  type = root.findPath(F_TYPE);
            UserGroup ug;

            ug = new UserGroup(XUUID.fromString(root.findPath(F_ID).asText()),
                               root.findPath(F_NAME).asText(),
                               root.findPath(F_CANONICAL).asText());

            if (type != null)
                ug.setGroupType(type.asText());

            return ug;
        }
        catch (IOException ix)
        {
            ix.printStackTrace();

            return null;
        }
    }

    /**
     * Getters
     */
    public XUUID getID()
    {
        return id;
    }

    /**
     * The concept of "system group" is fine, but the implementation is wonky.  All System groups
     * share a common prefix (SYSTYPE_PREFIX), and then the clients have control of the string
     * value after that.  However, it's nice to have that broken into 2 pieces:  the type and the
     * subtype.  An example type is "Role" and it would have multiple subtypes (ROOT, USER, etc.).
     * We need to be able to test if a UserGroup is a system group of type "Role" and we need to
     * be able to test if a UserGroup is a system group of type&subtype "Role.ROOT", hence these
     * methods...
     */
    public boolean isSystemGroup()
    {
        return (groupType != null) && groupType.startsWith(SYSTYPE_PREFIX);
    }
    public boolean isSystemGroupType(String type)
    {
        return isSystemGroup() && groupType.startsWith(type);
    }
    public boolean isSystemGroupSubtype(String subtype)
    {
        return isSystemGroup() && groupType.equals(subtype);
    }

    public String getName()
    {
        return groupName;
    }
    public String getCanonical()
    {
        return canonicalName;
    }
    public String getGroupType()
    {
        return groupType;
    }

    /**
     * Setters
     */
    public void setGroupType(String type)
    {
        // do NOT prepend prefix!  the clients should be doing all the management of this
        // (this should be revisited).
        groupType = type;
    }

    public String toJson()
    {
        ObjectNode  node = objectMapper.createObjectNode();

        node.put(F_ID,        id.toString());
        node.put(F_NAME,      groupName);
        node.put(F_CANONICAL, canonicalName);

        if (groupType != null)
            node.put(F_TYPE, groupType);

        return node.toString();
    }

    public String toString()
    {
        return "[group: id=" + id.toString() + ", name=" + groupName + ", canon=" + canonicalName + ", type=" + groupType + "]";
    }

}
