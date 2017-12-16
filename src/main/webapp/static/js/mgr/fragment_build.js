var editor = createEditor('editor');
var saveFlag = false;
var loadTemplates = function(){
	return [{name : '文章详情',path:rootPath+'/static/js/mgr/fragment_article.html'},{name : '上下文章',path:rootPath+'/static/js/mgr/fragment_articleNav.html'},
		{name:'文章列表',path:rootPath+'/static/js/mgr/fragment_articles.html'},{name:'文章统计',path:rootPath+'/static/js/mgr/fragment_articleStatistics.html'},
		{name:'文章标签',path:rootPath+'/static/js/mgr/fragment_articleTags.html'},{name:'评论',path:rootPath+'/static/js/mgr/fragment_comments.html'},
		{name:'评论统计',path:rootPath+'/static/js/mgr/fragment_commentStatistics.html'},{name:'评论挂件',path:rootPath+'/static/js/mgr/fragment_commentWidget.html'},
		{name:'底部',path:rootPath+'/static/js/mgr/fragment_foot.html'},{name:'最近评论',path:rootPath+'/static/js/mgr/fragment_lastComments.html'},
		{name:'密码锁',path:rootPath+'/static/js/mgr/fragment_lock_password.html'},{name:'问答锁',path:rootPath+'/static/js/mgr/fragment_lock_qa.html'},
		{name:'导航',path:rootPath+'/static/js/mgr/fragment_nav.html'},{name:'最近被访问文章',path:rootPath+'/static/js/mgr/fragment_recentlyViewdArticles.html'},
		{name:'标签统计',path:rootPath+'/static/js/mgr/fragment_tagStatistics.html'}]
}
$(document).ready(function() {
	$("input[name='global']").change(function(){
		if($(this).is(":checked")){
			$("#spaceSelector").hide();
		}else{
			$("#spaceSelector").show();
		}
	})
	editor.setSize('100%', $(window).height() - 30);
	sfq.setFileClickFunction(function(path){
		editor.insertUrl(path,true);
		return true;
	});
	
	$('#backupModal').on('show.bs.modal',function(){
		rewriteBaks();
	});
	
		$("#lookupModal").on("show.bs.modal", function() {
			showDataTags();
		});
		$('[data-handler]').click(function(){
			var m = $(this).attr("data-handler");
			switch(m){
			case 'file':
				fileSelectPageQuery(1,'');
	        	$("#fileSelectModal").modal("show");
				break;
			case 'localFile':
				sfq.show();
				break;
			case 'clear':
				bootbox.confirm("确定要清空吗？",function(result){
					if(result){
						editor.clear();
					}
				})
				break;
			case 'format':
				editor.format();
				break;
			case 'lookup':
				lookup();
				break;
			case 'template':
				showTemplateModal();
				break;
			case 'lock':
				showLock();
				break;
			default:
				break;
			}
		})
		$('a[data-toggle="tab"]').on('shown.bs.tab', function (e) {
			var html = '';
			var id = $(e.target).attr('id');
			switch(id){
			case "data-tab":
				showDataTags();
				break;
			case "fragment-tab":
				showUserFragment(1)
				break;
			}
		});
		
	});
	 $(document).keydown(function(e) {
	    if (e.ctrlKey && e.shiftKey && e.which === 70) {
	        editor.format()
	        return false;
	    }
	    return true;
	 });
	function addDataTag(name){
		editor.replaceSelection('<data name="'+name+'"/>');
		$("#lookupModal").modal('hide');
	}
	function addFragment(name){
		editor.replaceSelection('<fragment name="'+name+'"/>');
		$("#lookupModal").modal('hide')
	}
	
	function showLock(){
		$.get(basePath + '/mgr/lock/all',{},function(data){
			var oldLock = $("#oldLock").val();
			if(data.success){
				var locks = data.data;
				var html = '';
				if(locks.length > 0){
					html += '<div class="table-responsive">';
					html += '<table class="table">';
					for(var i=0;i<locks.length;i++){
						html += '<tr>';
						var lock = locks[i];
						html += '<tr>';
						html += '<td>'+lock.name+'</td>';
						html += '<td><a href="###" onclick="addLock(\''+lock.id+'\')"><span class="glyphicon glyphicon-ok-sign"></span></a></td>';
						html += '</tr>';
					}
					html += '</table>';
					html += '</div>';
					$("#lockBody").html(html);
					$("#lockModal").modal('show')
				} else {
					bootbox.alert("当前没有任何锁");
				}
			}else{
				console.log(data.data);
			}
		});
	}
	
	
	function showTemplateModal(){
		var templates = loadTemplates();
		if(!templates || templates.length == 0){
			bootbox.alert("沒有可供访问的地址");
			return ;
		}
		var html = "<div class='table-responsive'>";
		html += '<table class="table">';
		html += '<tr><td>模板名称</td><td></td></tr>';
		for(var i=0;i<templates.length;i++){
			html += '<tr><td>'+templates[i].name+'</td><td><a href="javascript:void(0)" onclick="loadTemplate(\''+templates[i].path+'\')">加载</a></td></tr>';
		}
		html += '</table>';
		html += '</div>';
		$("#templateModalBody").html(html);
		$("#templateModal").modal('show');
	}
	
	function loadTemplate(path){
		$.get(path,{},function(data){
			editor.setValue(data);
			$("#templateModal").modal('hide');
		})
	}
	
	function addLock(id){
		editor.replaceSelection('<lock id="'+id+'"/>');
		$("#lockModal").modal('hide')
	}
	
	function lookup(){
		$("#lookupModal").modal('show');
	}
	
	function showDataTags(){
		var html = '';
		$('[aria-labelledby="data-tab"]').html('<img src="'+basePath+'/static/img/loading.gif" class="img-responsive center-block"/>')
		$.get(basePath+"/mgr/template/dataTags",{},function(data){
			if(!data.success){
				bootbox.alert(data.message);
				return ;
			}
			data = data.data;
			html += '<div class=" table-responsive" style="margin-top:10px">';
			html += '<table class="table">';
			for(var i=0;i<data.length;i++){
				html += '<tr>';
				html += '<td>'+data[i]+'</td>';
				html += '<td><a onclick="addDataTag(\''+data[i]+'\')" href="###"><span class="glyphicon glyphicon-ok-sign" ></span>&nbsp;</a></td>';
				html += '</tr>';
			}
			html += '</table>';
			html += '</div>';
			$('[aria-labelledby="data-tab"]').html(html);
		});
	}
	
	function showUserFragment(i){
		var html = '';
		$('#fragment').html('<img src="'+basePath+'/static/img/loading.gif" class="img-responsive center-block"/>')
		$.get(basePath+"/mgr/template/fragment/list",{"currentPage":i},function(data){
			if(!data.success){
				bootbox.alert(data.message);
				return ;
			}
			var page = data.data;
			html += '<div class=" table-responsive" style="margin-top:10px">';
			html += '<table class="table">';
			for(var i=0;i<page.datas.length;i++){
				html += '<tr>';
				html += '<td>'+page.datas[i].name+'</td>';
				html += '<td><a onclick="addFragment(\''+page.datas[i].name+'\')" href="###"><span class="glyphicon glyphicon-ok-sign" ></span>&nbsp;</a></td>';
				html += '</tr>';
			}
			html += '</table>';
			html += '</div>';
			
			if(page.totalPage > 1){
				html += '<div>';
				html += '<ul class="pagination">';
				for(var i=page.listbegin;i<=page.listend-1;i++){
					html += '<li>';
					html += '<a href="###" onclick="showUserFragment(\''+i+'\')" >'+i+'</a>';
					html += '</li>';
				}
				html += '</ul>';
				html += '</div>';
			}
			$('#fragment').html(html);
		});
	}
	
	
	function save() {
		
	}
	
	
	var fragment_storage = (function() {
		
		var current_tpl;
		
		var getKey = function(){
			var key = $("#fragmentKey").val();
			if($.trim(key) == ''){
				key = "fragment_"+$.now();
				$("#fragmentKey").val(key);
			}
			return key;
		}
		
		setInterval(function(){
			if(saveFlag) return ;
			var content = editor.getValue();
			if($.trim(content) != ''){
				var time = $.now();
				local_storage.store(getKey(),JSON.stringify({"id":getKey(),"content":content,"time":time}));
				$("#auto-save-timer").html("最近备份："+new Date(time).format('yyyy-mm-dd HH:MM:ss'))
			}
			else
				fragment_storage.removeCurrent();
		},15000);
		
		var v = local_storage.get(getKey());
		if(v != null){
			v = $.parseJSON(v);
			current_tpl = editor.getValue();
			bootbox.confirm("系统发现在"+new Date(v.time).format('yyyy-mm-dd HH:MM:ss')+"留有备份，是否加载？",function(result){
				if(result){
					editor.setValue(v.content);
				}
			})
		}
		
		return {
			listAll:function(){
				var arr = [];
				local_storage.each(function(key,v){
					if(key.indexOf('fragment_') > -1){
						arr.push({'key':key,"value":v});
					}
				});
				arr.sort(function(x,y){
					var v1 = $.parseJSON(x.value);
					var v2 = $.parseJSON(y.value);
					return  -v1.time + v2.time;
				})
				return arr;
			},
			get:function(key){
				return local_storage.get(key);
			},
			remove:function(key){
				local_storage.remove(key);
			},
			removeCurrent:function(){
				var key = $("#fragmentKey").val();
				if($.trim(key) != ''){
					local_storage.remove(key);
				}
			}
		}
	}());
	
	function loadBak(key){
		bootbox.confirm("确定要加载吗",function(result){
			if(result){
				var v = fragment_storage.get(key);
				if(v != null){
					v = $.parseJSON(v);
					$("#fragmentKey").val(v.id);
					editor.setValue(v.content);
				}
				$("#backupModal").modal('hide');
			}
		})
	}
	
	function rewriteBaks(){
		var baks = fragment_storage.listAll();
		var html = '<div class="table-responsive">';
		html += '<table class="table">';
		html += '<tr><th>ID</th><th>时间</th><th>操作</th></tr>'
		for(var i=0;i<baks.length;i++){
			var v = $.parseJSON(baks[i].value);
			html += '<tr><td>'+v.id+'</td><td>'+new Date(v.time).format('yyyy-mm-dd HH:MM:ss')+'</td><td><a href="###" onclick="loadBak(\''+v.id+'\')" style="margin-right:5px">加载</a><a href="###" onclick="delBak(\''+v.id+'\')">删除</a></td></tr>';
		}
		html += '</table>';
		html += '</div>';
		$("#backup-body").html(html);
	}
	
	function delBak(key){
		bootbox.confirm("确定要删除吗",function(result){
			if(result){
				fragment_storage.remove(key);
				rewriteBaks();
			}
		})
	}
	
	function save(){
		var data = $("#previewModal form").serializeObject();
		var space = data.space;
		delete data['space'];
		if(space != ''){
			data.space = {"id":space};
		}
		data.global = $("#previewModal form input[type=checkbox]").eq(0).prop("checked");
		data.callable = $("#previewModal form input[type=checkbox]").eq(1).prop("checked");
		data.tpl = editor.getValue();
		
		var id = $("#fragmentId").val();
		var isSave = true;
		var url = basePath + "/mgr/template/fragment/create";
		if(id != ''){
			isSave = false;
			url = basePath + "/mgr/template/fragment/update";
		}
		if(!isSave){
			data.id = id;
		}
		saveFlag = true;
		$.ajax({
			type : "post",
			url : url,
			data : JSON.stringify(data),
			dataType : "json",
			contentType : 'application/json',
			success : function(data) {
				if (data.success) {
					bootbox.alert("保存成功");
					fragment_storage.removeCurrent();
					if(isSave){
						$("#fragmentId").val(data.data.id);
						$("#fragmentKey").val("fragment_"+data.data.id)
					}
					$("#previewModal").modal('hide');
				} else {
					bootbox.alert(data.message);
				}
			},
			complete : function() {
				saveFlag = false;
			}
		});
	}
