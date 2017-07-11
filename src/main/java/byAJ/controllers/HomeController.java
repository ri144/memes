package byAJ.controllers;

import byAJ.configs.CloudinaryConfig;
import byAJ.models.Photo;
import byAJ.models.User;
import byAJ.repositories.PhotoRepository;
import byAJ.services.UserService;
import byAJ.validators.UserValidator;
import com.cloudinary.Singleton;
import com.cloudinary.StoredFile;
import com.cloudinary.utils.ObjectUtils;
import org.apache.commons.collections.map.HashedMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.cloudinary.Cloudinary;

import javax.validation.Valid;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Controller
public class HomeController {

    @Autowired
    CloudinaryConfig cloudc;

    @Autowired
    private UserValidator userValidator;

    @Autowired
    private UserService userService;

    @Autowired
    private PhotoRepository photoRepo;

    @RequestMapping("/")
    public String index(){
        return "index";
    }

    @RequestMapping("/login")
    public String login(){
        return "login";
    }

    @RequestMapping(value="/register", method = RequestMethod.GET)
    public String showRegistrationPage(Model model){
        model.addAttribute("user", new User());
        return "registration";
    }

    @RequestMapping(value="/register", method = RequestMethod.POST)
    public String processRegistrationPage(@Valid @ModelAttribute("user") User user, BindingResult result, Model model){

        model.addAttribute("user", user);
        userValidator.validate(user, result);

        if (result.hasErrors()) {
            return "registration";
        } else {
            userService.saveUser(user);
            model.addAttribute("message", "User Account Successfully Created");
        }
        return "index";
    }

    public UserValidator getUserValidator() {
        return userValidator;
    }

    public void setUserValidator(UserValidator userValidator) {
        this.userValidator = userValidator;
    }

    @GetMapping("/upload")
    public String uploadForm(Model model) {
        model.addAttribute("p", new Photo());
        return "upload";
    }

    @PostMapping("/upload")
    public String singleImageUpload(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes, Model model, @ModelAttribute Photo p){

        if (file.isEmpty()){
            redirectAttributes.addFlashAttribute("message","Please select a file to upload");
            return "redirect:uploadStatus";
        }

        try {
            Map uploadResult =  cloudc.upload(file.getBytes(), ObjectUtils.asMap("resourcetype", "auto"));

            model.addAttribute("message",
                    "You successfully uploaded '" + file.getOriginalFilename() + "'");
            String filename = uploadResult.get("public_id").toString() + "." + uploadResult.get("format").toString();
            p.setImage("<img src='http://res.cloudinary.com/dop68xspe/image/upload/"+filename+"'/>");
            System.out.printf("%s\n", cloudc.createUrl(filename,900,900, "fit"));
            p.setCreatedAt(new Date());
            photoRepo.save(p);
            setupGallery(model);
        } catch (IOException e){
            e.printStackTrace();
            model.addAttribute("message", "Sorry I can't upload that!");
        }
        return "gallery";
    }
    @RequestMapping("/img/{id}")
    public String something(@PathVariable("id") long id, Model model){
        model.addAttribute("photo", photoRepo.findById(id));
        return "textgen";
    }
    @RequestMapping("/gallery")
    public String gallery(Model model){
        setupGallery(model);
        return "gallery";
    }

    @RequestMapping("/textgen")
    public String textgen(Model model){
        model.addAttribute("photo", new Photo());
        return "textgen";
    }

    @PostMapping("/creatememe")
    public String creatememe(@ModelAttribute Photo photo, Model model){
        photoRepo.save(photo);
        setupGallery(model);
        model.addAttribute("Meme created");
        return "gallery";
    }

    @RequestMapping("/select/{id}")
    public String selectSomthign(@PathVariable("id") String type, Model model){
                List<Photo> list = photoRepo.findAllByType(type);
                model.addAttribute("images", list);
                return "makememe";
    }

    @GetMapping("/makememe")
    public String getMeme(Model model){
        Iterable<Photo> list = photoRepo.findAll();
        List<Photo> list2 = new ArrayList<Photo>();
        for(Photo p : list){
            boolean check = true;
            for(Photo p2 : list2){
                if(p2.getType().equals(p.getType())){
                    //System.out.printf("%s %s\n", p2.getType(), p.getType());
                    check = false;
                    break;
                }
                else{
                    //System.out.printf("%s %s\n", p2.getType(), p.getType());
                    check = true;
                }
            }
            if(check){
                list2.add(p);

            }
           // System.out.printf("%s\n", p.getType());
        }
        Set<Photo> myList = new HashSet<Photo>();
        for(Photo p2 : list2){
            System.out.printf("%s\n", p2.getType());
            myList.add(p2);
        }


        model.addAttribute("photoList", myList);
        return "makememe";
    }

    private void setupGallery(Model model){
        Iterable<Photo> photoList = photoRepo.findAllByBotmessageIsNotAndTopmessageIsNot("","");

        model.addAttribute("images", photoList);
    }

}
