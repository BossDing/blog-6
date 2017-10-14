package me.qyh.blog.web.controller.back;

import java.util.HashSet;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import me.qyh.blog.core.config.Constants;
import me.qyh.blog.core.exception.LogicException;
import me.qyh.blog.core.message.Message;
import me.qyh.blog.core.vo.JsonResult;
import me.qyh.blog.file.store.local.EditablePathResourceHttpRequestHandler;
import me.qyh.blog.file.validator.LocalFileQueryParamValidator;
import me.qyh.blog.file.validator.LocalFileUploadValidator;
import me.qyh.blog.file.vo.LocalFileQueryParam;
import me.qyh.blog.file.vo.LocalFileUpload;
import me.qyh.blog.file.vo.UnzipConfig;
import me.qyh.blog.file.vo.UploadedFile;

@Controller
@RequestMapping("mgr/localFile")
public class LocalFileMgrController extends BaseMgrController {

	@Autowired(required = false)
	private EditablePathResourceHttpRequestHandler handler;
	
	@Autowired
	private LocalFileQueryParamValidator localFileParamValidator;
	@Autowired
	private LocalFileUploadValidator localFileUploadValidator;

	@InitBinder(value = "localFileQueryParam")
	protected void initLocalFileQueryParamBinder(WebDataBinder binder) {
		binder.setValidator(localFileParamValidator);
	}

	@InitBinder(value = "localFileUpload")
	protected void initLocalUploadBinder(WebDataBinder binder) {
		binder.setValidator(localFileUploadValidator);
	}

	@GetMapping("index")
	public String index(@Validated LocalFileQueryParam localFileQueryParam,Model model) {
		try {
			checkHandler();
			model.addAttribute("result", handler.query(localFileQueryParam));
		} catch (LogicException e) {
			model.addAttribute(Constants.ERROR, e.getLogicMessage());
		}
		return "mgr/file/local";
	}
	

	@GetMapping("query")
	@ResponseBody
	public JsonResult query(@Validated LocalFileQueryParam localFileQueryParam) throws LogicException {
		checkHandler();
		localFileQueryParam.setQuerySubDir(false);
		localFileQueryParam.setExtensions(new HashSet<>());
		return new JsonResult(true, handler.query(localFileQueryParam));
	}
	
	@PostMapping("upload")
	@ResponseBody
	public JsonResult upload(@Validated LocalFileUpload localFileUpload, BindingResult result) throws LogicException {
		checkHandler();
		if (result.hasErrors()) {
			List<ObjectError> errors = result.getAllErrors();
			for (ObjectError error : errors) {
				return new JsonResult(false,
						new Message(error.getCode(), error.getDefaultMessage(), error.getArguments()));
			}
		}
		List<UploadedFile> uploadedFiles = handler.upload(localFileUpload);
		return new JsonResult(true, uploadedFiles);
	}
	
	@PostMapping("copy")
	@ResponseBody
	public JsonResult copy(@RequestParam("path") String path, @RequestParam("destPath") String destPath)
			throws LogicException {
		checkHandler();
		handler.copy(path, destPath);
		return new JsonResult(true, new Message("file.copy.success", "拷贝成功"));
	}

	@PostMapping("move")
	@ResponseBody
	public JsonResult move(@RequestParam("path") String path, @RequestParam("destPath") String destPath)
			throws LogicException {
		checkHandler();
		handler.move(path, destPath);
		return new JsonResult(true, new Message("file.move.success", "移动成功"));
	}
	
	@PostMapping("delete")
	@ResponseBody
	public JsonResult delete(@RequestParam("path") String path) throws LogicException {
		checkHandler();
		handler.delete(path);
		return new JsonResult(true, new Message("file.delete.success", "删除成功"));
	}

	
	@PostMapping("createFolder")
	@ResponseBody
	public JsonResult createFolder(@RequestParam("path") String path) throws LogicException {
		checkHandler();
		handler.createDirectorys(path);
		return new JsonResult(true, new Message("file.create.success", "创建成功"));
	}
	
	@PostMapping("unzip")
	@ResponseBody
	public JsonResult unzip( 
			@RequestParam("zipPath") String zipPath,UnzipConfig config) throws LogicException{
		checkHandler();
		if(config.getPath() == null){
			return new JsonResult(false, new Message("file.unzip.emptyPath", "zip文件路径不能为空"));
		}
		handler.unzip(zipPath,config);
		return new JsonResult(true, new Message("file.unzip.success", "解压缩成功"));
	}
	
	private void checkHandler() throws LogicException {
		if (handler == null) {
			throw new LogicException("handler.notEnable", "本地文件服务没有启用");
		}
	}

}
