package usjobs.web.controller;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import usjobs.model.Employer;
import usjobs.model.JobPosting;
import usjobs.model.JobSeeker;
import usjobs.model.User;
import usjobs.model.dao.JobPostingDao;
import usjobs.model.dao.UserDao;
import usjobs.util.Security;

@Controller
@SessionAttributes({"newJob", "editJob"})
public class ProfileController {
	
	@Autowired
	UserDao userDao;
	
	@Autowired
	JobPostingDao jobPostingDao;
	
    /**
     * Return the correct profile page for the user depending on if they are
     * an employer, seeker, or admin.
     * @return
     */
    @RequestMapping(value = "/user/profile.html", method = RequestMethod.GET)
    public String getProfile(ModelMap models) {
    	UserDetails details = Security.getUserDetails();
    	User user = userDao.getProfileUser(details.getUsername());
    	
    	if (user.isAdmin()) {
    		return "profile/admin";
    	}
    	
    	if (user.isEmployer()) {
    		Employer employer = (Employer) user;
    		models.put("user", employer);
    		JobPosting newJob = new JobPosting();
    		newJob.setCompany(employer);
    		models.put("newJob", newJob);
    		return "profile/employer";
    	}
    	
    	if (user.isSeeker()){
    		JobSeeker seeker = (JobSeeker) user;
    		models.put("user", seeker);
    		return "profile/job-seeker";
    	}
    	
    	//This would protect against some db insertion? mistake that caused this authenticated user to have no role at all.
    	//then just redirect them to home page.
    	return "home.html";
    }
    
	@RequestMapping(value = "/user/addJob.html", method = RequestMethod.POST)
	public String addJob(@RequestParam int employerId, @ModelAttribute("newJob") JobPosting newJob, SessionStatus session) {
		Date date = new Date();
		newJob.setDatePosted(date);
		newJob.setEnabled(true);
		newJob.setOpened(true);
		jobPostingDao.save(newJob);
		session.setComplete();
		return "redirect:profile.html?id=" + employerId;
	}

	@RequestMapping(value = "/user/deleteJob.html", method = RequestMethod.POST)
	public String deleteJob(@RequestParam int employerId, @RequestParam int jobId) {
		JobPosting jobPosting = jobPostingDao.getJobPosting(jobId);
		jobPostingDao.delete(jobPosting);
		return "redirect:profile.html?id=" + employerId;
	}
	
	@RequestMapping(value = "/user/toggleJob.html", method = RequestMethod.POST)
	public String toggleJob(@RequestParam int employerId, @RequestParam int jobId) {
		JobPosting jobPosting = jobPostingDao.getJobPosting(jobId);
		boolean isOpened = jobPosting.isOpened();
		if (isOpened) {
			jobPosting.setDateClosed(new Date());
		} else {
			jobPosting.setDatePosted(new Date());
		}
		jobPosting.setOpened(!isOpened);
		jobPostingDao.save(jobPosting);
		return "redirect:profile.html?id=" + employerId;
	}

	@RequestMapping(value = "/user/editJob.html", method = RequestMethod.GET)
	public String editJobForm(@RequestParam int employerId, @RequestParam int jobId, ModelMap models) {
		models.put("editJob", jobPostingDao.getJobPosting(jobId));
		return "job/edit";
	}

	@RequestMapping(value = "/user/editJob.html", method = RequestMethod.POST)
	public String editJobForm(@RequestParam int employerId, @ModelAttribute("editJob") JobPosting editJob, SessionStatus sessionStatus) {
		jobPostingDao.save(editJob);
		sessionStatus.setComplete();
		return "redirect:profile.html?id=" + employerId;
	}
}
