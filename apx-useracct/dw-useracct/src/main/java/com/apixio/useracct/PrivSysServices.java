package com.apixio.useracct;

import com.apixio.security.Security;
import com.apixio.useracct.config.MessengerConfig;
import com.apixio.useracct.messager.AWSSNSMessenger;
import com.apixio.useracct.messager.Messenger;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import com.apixio.restbase.DaoBase;
import com.apixio.SysServices;
import com.apixio.aclsys.buslog.PrivAccessTypeLogic;
import com.apixio.aclsys.buslog.PrivAclLogic;
import com.apixio.aclsys.buslog.PrivOperationLogic;
import com.apixio.aclsys.dao.PrivAccessTypes;
import com.apixio.aclsys.dao.PrivOperations;
import com.apixio.useracct.buslog.AuthLogic;
import com.apixio.useracct.buslog.PrivRoleLogic;
import com.apixio.useracct.buslog.PrivUserLogic;
import com.apixio.useracct.buslog.SysStateLogic;
import com.apixio.useracct.buslog.VerifyLogic;
import com.apixio.useracct.config.EmailConfig;
import com.apixio.useracct.dao.OldPrivRoles;
import com.apixio.useracct.dao.PrivUsers;
import com.apixio.useracct.dao.SysStates;
import com.apixio.useracct.dao.VerifyLinks;
import com.apixio.useracct.dw.ServiceConfiguration;
import com.apixio.useracct.email.Mailer;
import com.apixio.useracct.email.TemplateManager;

/**
 * SysServices is the central location/class to store all system level singleton objects
 * that provide a service to another object (or objects).
 *
 * The basic types of objects/services that belong here are:
 *
 *  1.  data access services/objects
 *  2.  utility objects
 *  3.  business logic objects
 *
 * One architectural goal is to have some ordering/layering of these services so the initialization
 * of these objects should be done in the above numbered order:  first DAOs, then utilities, then
 * business logic.  No DAO should depend on a business object, etc.  (NOTE!  it might be necessary
 * to have a small set of utility objects initialized prior to DAOs...)
 */
public class PrivSysServices extends SysServices
{
    // DataAccessObjects:
    private PrivAccessTypes privAccessTypes;
    private PrivOperations  privOperations;
    private OldPrivRoles    oldPrivRoles;
    private PrivUsers       privUsers;
    private SysStates       sysStates;
    private VerifyLinks     verifyLinks;

    // Miscellaneous
    private Mailer          mailer;
    private TemplateManager templateManager;
    private Messenger       messenger;

    // BusinessLogic:
    private AuthLogic           authLogic;
    private PrivAccessTypeLogic privAccessTypeLogic;
    private PrivAclLogic        privAclLogic;
    private PrivOperationLogic  privOperationLogic;
    private PrivRoleLogic       privRoleLogic;
    private PrivUserLogic       privUserLogic;
    private SysStateLogic       sysStateLogic;
    private VerifyLogic         verifyLogic;

    public PrivSysServices(DaoBase seed, boolean fullServices, ServiceConfiguration config) throws Exception
    {
        super(seed, config);

        // ################################################################
        // Required order of initialization:
        //
        //  1.  Data Access Objects.  These MUST be initialized first and must depend ONLY
        //      on persistence-level objects
        //
        //  2.  Miscellaneous objects that business objects depend on (e.g., utilities)
        //
        //  3.  Business Logic objects.  These MUST NOT depend on anything other than
        //      DAOs and utilities
        //
        // Note that SysServices collects all the above into a single object.  Initialization
        // of anything other than DAOs will pass in 'this' because of the dependence on
        // SysServices.

        // ################################################################
        //  DAO initialization

        // A bit of a hack (I really do prefer Spring's IoC model...) in that the
        // DAO model suggests that we use an already-setup Persistable (meaning
        // that it has RedisOps and key prefix) to set up more Persistable-derived
        // classes.
        this.privAccessTypes = new PrivAccessTypes(seed);
        this.privOperations  = new PrivOperations(seed);
        this.oldPrivRoles    = new OldPrivRoles(seed);
        this.privUsers       = new PrivUsers(seed);
        this.sysStates       = new SysStates(seed);

        //!!!! all of these "config != null" tests are a big hack to support a cmdline
        // bootup that doesn't contain/require all the normal yaml config.  yuck (for now
        // I'll leave it but it needs to be fixed)
        if (fullServices)
            this.verifyLinks     = new VerifyLinks(seed, config.getVerifyLinkConfig());

        // ################################################################
        //  Utility initialization
        if (fullServices)
            setupMailer(config);

        setupMessenger(config);
        // ################################################################
        //  Business logic initialization
        if (fullServices)
            this.authLogic        = addPostInit(new AuthLogic(this, config));

        this.privAccessTypeLogic  = new PrivAccessTypeLogic(this);
        this.privAclLogic         = new PrivAclLogic(this);
        this.privOperationLogic   = new PrivOperationLogic(this);
        this.privRoleLogic        = addPostInit(new PrivRoleLogic(this));
        if (fullServices)
            this.privUserLogic    = addPostInit(new PrivUserLogic(this, config));
        this.sysStateLogic        = addPostInit(new SysStateLogic(this));
        this.verifyLogic          = addPostInit(new VerifyLogic(this, config));

        // now set the privileged logic objects in the main SysServices
        // (required minimally because of caches kept in the logic objects).
        // O Spring, where art thou, with your singletons?
        super.setAccessTypeLogic(this.privAccessTypeLogic);
        super.setOperationLogic(this.privOperationLogic);
        super.setRoleLogic(this.privRoleLogic);
        super.setUserLogic(this.privUserLogic);

        super.doPostInit();
    }

    private void setupMessenger(ServiceConfiguration config) throws Exception
    {
        MessengerConfig messengerConfig = config.getMessengerConfig();

        if (messengerConfig != null){
            Security security = Security.getInstance();
            String accessKey = security.decrypt(messengerConfig.getEncryptedAccessKey());
            String secretKey = security.decrypt(messengerConfig.getEncryptedSecretAccessKey());
            String regions = messengerConfig.getRegion();
            String serviceType = messengerConfig.getServiceType();
            String messageType = messengerConfig.getMessageType();

            if (serviceType!=null && serviceType.equals(AWSSNSMessenger.AWSSNS)) {
                this.messenger = new AWSSNSMessenger(accessKey, secretKey, regions, messageType);
            }
        }
    }

    private void setupMailer(ServiceConfiguration config) throws Exception
    {
        EmailConfig emailConfig = config.getEmailConfig();

        if (emailConfig != null)
        {
            JavaMailSenderImpl javaSender = new JavaMailSenderImpl();

            this.mailer = new Mailer();
            mailer.setDefaultSenderAddress(emailConfig.getDefaultSender());

            javaSender.setHost(emailConfig.getSmtpHost());
            javaSender.setPort(emailConfig.getSmtpPort());
            javaSender.setUsername(emailConfig.getSmtpUsername());
            javaSender.setPassword(emailConfig.getSmtpPassword());
            javaSender.setJavaMailProperties(emailConfig.getJavaMailProperties());

            mailer.setMailSender(javaSender);

            this.templateManager = new TemplateManager(this, emailConfig.getTemplates());
        }
    }

    // DAO
    public PrivAccessTypes getPrivAccessTypes()  { return privAccessTypes; }
    public PrivOperations  getPrivOperations()   { return privOperations;  }
    public OldPrivRoles    getOldPrivRoles()     { return oldPrivRoles;    }
    public PrivUsers       getPrivUsers()        { return privUsers;       }
    public SysStates       getSysStates()        { return sysStates;       }
    public VerifyLinks     getVerifyLinks()      { return verifyLinks;     }

    // utility
    public Mailer          getMailer()           { return mailer;           }
    public TemplateManager getTemplateManager()  { return templateManager;  }
    public Messenger       getMessenger()        { return messenger;        }

    // business logic
    public AuthLogic           getAuthLogic()            { return authLogic;           }
    public PrivAccessTypeLogic getPrivAccessTypeLogic()  { return privAccessTypeLogic; }
    public PrivAclLogic        getPrivAclLogic()         { return privAclLogic;        }
    public PrivOperationLogic  getPrivOperationLogic()   { return privOperationLogic;  }
    public PrivRoleLogic       getPrivRoleLogic()        { return privRoleLogic;       }
    public PrivUserLogic       getPrivUserLogic()        { return privUserLogic;       }
    public SysStateLogic       getSysStateLogic()        { return sysStateLogic;       }
    public VerifyLogic         getVerifyLogic()          { return verifyLogic;         }

}
