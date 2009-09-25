package fi.hut.soberit.agilefant.web;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.opensymphony.xwork2.Action;

import fi.hut.soberit.agilefant.business.DailyWorkBusiness;
import fi.hut.soberit.agilefant.business.TaskBusiness;
import fi.hut.soberit.agilefant.business.UserBusiness;
import fi.hut.soberit.agilefant.exception.ObjectNotFoundException;
import fi.hut.soberit.agilefant.model.Task;
import fi.hut.soberit.agilefant.model.User;
import fi.hut.soberit.agilefant.model.WhatsNextEntry;
import fi.hut.soberit.agilefant.transfer.DailyWorkTaskTO;

public class DailyWorkActionTest {
    private DailyWorkAction testable;

        
    private DailyWorkBusiness dailyWorkBusiness;
    private UserBusiness userBusiness;
    private TaskBusiness taskBusiness;

    protected int LOGGED_IN_USER = 2;
    
    @SuppressWarnings("serial")
    @Before
    public void setUp_dependencies() {
        testable = new DailyWorkAction() {
            @Override
            protected int getLoggedInUserId() {
                return LOGGED_IN_USER ;
            }
        };
        
        dailyWorkBusiness = createStrictMock(DailyWorkBusiness.class);
        testable.setDailyWorkBusiness(dailyWorkBusiness);

        userBusiness = createStrictMock(UserBusiness.class);
        testable.setUserBusiness(userBusiness);

        taskBusiness = createStrictMock(TaskBusiness.class);
        testable.setTaskBusiness(taskBusiness);
    }
    
    private void replayAll() {
        replay(dailyWorkBusiness, taskBusiness, userBusiness);
    }

    private void verifyAll() {
        verify(dailyWorkBusiness, taskBusiness, userBusiness);
    }
    
    @Test
    public void testRetrieve() {
        User user = new User(); 
        testable.setUserId(1);

        Collection<DailyWorkTaskTO> returnedList  = Arrays.asList(
            new DailyWorkTaskTO(new Task(), DailyWorkTaskTO.TaskClass.ASSIGNED, -1), 
            new DailyWorkTaskTO(new Task(), DailyWorkTaskTO.TaskClass.NEXT, 1), 
            new DailyWorkTaskTO(new Task(), DailyWorkTaskTO.TaskClass.NEXT, 2), 
            new DailyWorkTaskTO(new Task(), DailyWorkTaskTO.TaskClass.ASSIGNED, -1), 
            new DailyWorkTaskTO(new Task(), DailyWorkTaskTO.TaskClass.NEXT, 4)
        );

        List<User> users = getUserList();
        User u1 = users.get(0);
        User u2 = users.get(1);

        expect(userBusiness.retrieve(1)).andReturn(user);
        expect(userBusiness.getEnabledUsers()).andReturn(users);
        expect(dailyWorkBusiness.getAllCurrentTasksForUser(user))
            .andReturn(returnedList);

        replayAll();

        assertEquals(Action.SUCCESS, testable.retrieve());

        verifyAll();

        assertEquals(returnedList,  testable.getAssignedTasks());
        
        Collection<User> usersReturned = testable.getEnabledUsers();
        assertEquals(usersReturned.size(), 2);
        assertTrue(usersReturned.contains(u1));
        assertTrue(usersReturned.contains(u2));
    }

    @Test(expected=ObjectNotFoundException.class)
    public void testRetrieve_withDefaultUserAndUserNotFound() {
        expect(userBusiness.retrieve(LOGGED_IN_USER)).andThrow(new ObjectNotFoundException());
        replayAll();
        testable.retrieve();
        verifyAll();
    }
    
    @Test
    public void testDeleteFromQueue() {
        User user = new User();
        user.setId(LOGGED_IN_USER);
        
        Task task = new Task();
        task.setId(1);
        
        testable.setTaskId(1);
        
        expect(userBusiness.retrieve(LOGGED_IN_USER)).andReturn(user);
        expect(taskBusiness.retrieve(1)).andReturn(task);
        dailyWorkBusiness.removeFromWhatsNext(user, task);

        replayAll();
        testable.deleteFromWorkQueue();
        
        verifyAll();
        
        // This is to be provided in JSON
        assertSame(task, testable.getTask());
    }

    @Test
    public void testAddToQueue() {
        User user = new User();
        user.setId(3);
        
        Task task = new Task();
        task.setId(1);
        
        testable.setTaskId(1);
        testable.setUserId(3);
        
        expect(userBusiness.retrieve(3)).andReturn(user);
        expect(taskBusiness.retrieve(1)).andReturn(task);
        
        WhatsNextEntry entry = new WhatsNextEntry();
        expect(dailyWorkBusiness.addToWhatsNext(user, task)).andReturn(entry);

        replayAll();
        testable.addToWorkQueue();
        
        verifyAll();
        
        // This is to be provided in JSON
        assertSame(task, testable.getTask());
    }
    
    @Test
    public void testRankQueueTaskAndMoveUnder() {
        Task task = new Task();
        task.setId(1);
        
        Task rankUnder = new Task();
        rankUnder.setId(2);
        
        User user = new User();
        user.setId(3);
        
        testable.setTaskId(1);
        testable.setRankUnderId(2);
        testable.setUserId(3);

        expect(userBusiness.retrieve(3)).andReturn(user);
        expect(taskBusiness.retrieve(1)).andReturn(task);
        expect(taskBusiness.retrieveIfExists(2)).andReturn(rankUnder);
        expect(dailyWorkBusiness.rankUnderTaskOnWhatsNext(user, task, rankUnder)).andReturn(new DailyWorkTaskTO(task));
        
        replayAll();
        
        testable.rankQueueTaskAndMoveUnder();
        
        verifyAll();
    }

    private List<User> getUserList() {
        List<User> users = new ArrayList<User>();
        User u1 = new User();
        u1.setId(5);
        u1.setFullName("Antti Haapala Sr");

        User u2 = new User();
        u1.setId(9);
        u1.setFullName("Antti Haapala Jr");

        users.add(u1);
        users.add(u2);
        return users;
    }
    
}
