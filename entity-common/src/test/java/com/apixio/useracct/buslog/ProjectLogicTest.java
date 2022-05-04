package com.apixio.useracct.buslog;

import com.apixio.SysServices;
import com.apixio.datasource.redis.DistLock;
import com.apixio.restbase.DaoBase;
import com.apixio.useracct.dao.Projects;
import com.apixio.useracct.entity.Project;
import org.junit.Before;
import org.junit.Test;
import org.junit.Assert;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ProjectLogic.class, SysServices.class, DistLock.class, Projects.class, DaoBase.class})
public class ProjectLogicTest {


    private ProjectLogic projectLogic = PowerMockito.mock(ProjectLogic.class);
    private Project project = PowerMockito.mock(Project.class);
    private Projects projects = PowerMockito.mock(Projects.class);

    @Before
    public void initialize()
    {
        Map<String, Object> getProjectByIDLikeUseracctMap = new HashMap<>();
        getProjectByIDLikeUseracctMap.put("happy", "lemon");
        PowerMockito.when(projectLogic.getProjectByIDLikeUseracct(project)).thenReturn(getProjectByIDLikeUseracctMap);
        PowerMockito.doCallRealMethod().when(projectLogic).buildStoringMap(any(), any(), any(), any(), any());
        PowerMockito.doCallRealMethod().when(projectLogic).getProjectByIDLikeUseracctString(project);
        PowerMockito.doCallRealMethod().when(projects).makeKey(anyString());
    }

    @Test
    public void testBuildStoringMap()
    {
        Map<String, String> someMap = null;
        try {
            someMap = projectLogic.buildStoringMap(project, "", "whatever@apixio.com", "some_method", "some_comment");
        } catch (Exception e) {
            assert false: String.format("should not throw exception during the test. Method: %s. Error message: %s", "testGetProjectByIDLikeUseracctString", e.getMessage());
        }
        Assert.assertNotNull (someMap);
        Assert.assertEquals  (someMap.containsKey(ProjectLogic.LAST_CREATED_AT), true);
        Assert.assertEquals  (someMap.containsKey(ProjectLogic.PROJECT_DATA), true);
        Assert.assertEquals  (someMap.containsKey(ProjectLogic.LAST_UPDATED_BY), true);
        Assert.assertEquals  (someMap.containsKey(ProjectLogic.METHOD), true);
        Assert.assertEquals  (someMap.containsKey(ProjectLogic.COMMENT), true);
        Assert.assertEquals  (someMap.containsKey(ProjectLogic.VERSION), true);
        Assert.assertEquals  (someMap.get(ProjectLogic.PROJECT_DATA), "{\"happy\":\"lemon\"}");

    }

    @Test
    public void testGetProjectByIDLikeUseracctString()
    {
        String str = null;
        try {
            str = projectLogic.getProjectByIDLikeUseracctString(project);
        } catch (Exception e) {
            assert false: String.format("should not throw exception during the test. Method: %s. Error message: %s", "testGetProjectByIDLikeUseracctString", e.getMessage());
        }
        Assert.assertNotNull (str);
        Assert.assertEquals ("{\"happy\":\"lemon\"}", str);
    }

    @Test
    public void testGetPropertyBag()
    {
        ProjectLogic projectLogic = new ProjectLogic(null);
        Assert.assertEquals(projectLogic.getPropertyBag("gen"), ProjectLogic.PropertyBag.GENERIC);
        Assert.assertEquals(projectLogic.getPropertyBag("generic"), ProjectLogic.PropertyBag.GENERIC);
        Assert.assertEquals(projectLogic.getPropertyBag("hcc"), ProjectLogic.PropertyBag.HCC);
        Assert.assertEquals(projectLogic.getPropertyBag("phase"), ProjectLogic.PropertyBag.PHASE);
        Assert.assertEquals(projectLogic.getPropertyBag("quality"), ProjectLogic.PropertyBag.QUALITY);
        Assert.assertEquals(projectLogic.getPropertyBag("prospective"), ProjectLogic.PropertyBag.PROSPECTIVE);
        try {
            projectLogic.getPropertyBag("unknownBagName");
            Assert.fail( "should throw exception other than known bag name" );
        } catch (IllegalArgumentException expectedException) {
        }
    }
}
