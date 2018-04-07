 var cmt = (function(config) {
	 	$("<style type='text/css'> .media-content{word-break: break-all;} .media-content img {max-width: 100%; height: auto;}  </style>").appendTo("head");
        var moduleId;
        var parentId;
        var moduleType;
        var commentFunction;
        var modal = '<div class="modal" tabindex="-1" role="dialog" id="comment-modal">';
        modal += '<div class="modal-dialog" role="document">';
        modal += '<div class="modal-content">';
        modal += '<div class="modal-header">';
        modal += '<button type="button" class="close" data-dismiss="modal" aria-label="Close">';
        modal += '<span aria-hidden="true">&times;</span>';
        modal += '</button>';
        modal += '<h4 class="modal-title">评论</h4>';
        modal += '</div>';
        modal += '<div class="modal-body">';
        modal += '<div class="alert alert-danger" style="display: none" id="comment-error-tip"></div>';
        modal += '<form class="form-horizontal">';
        if (!config.isLogin) {
            modal += '<div class="form-group" >';
            modal += '<label class="col-sm-2 control-label">昵称</label>';
            modal += '<div class="col-sm-10">';
            modal += ' <input type="text" class="form-control" id="nickname" placeholder="必填">';
            modal += ' </div>';
            modal += '</div>';
        }
        modal += '<div class="form-group" >';
        modal += '<label class="col-sm-2 control-label">内容</label>';
        modal += '<div class="col-sm-10">';
        modal += '<textarea class="form-control" id="content" style="height: 270px" placeholder="必填"></textarea>';
        modal += '</div>';
        modal += '</div>';
        
        modal += '<div class="form-group" style="display:none" id="captchaContainer">';
        modal += '<label class="col-sm-2 control-label"></label>';
        modal += '<div class="col-sm-10">';
        modal += '<img src="'+basePath+'/captcha" class="img-responsive" id="captcha-img"/>'
        modal += ' <input type="text" class="form-control" id="comment-captcha" placeholder="验证码">';
        modal += '</div>';
        modal += '</div>';
        
        if (!config.isLogin) {
            modal += '<p class="text text-info" style="text-align: right">';
            modal += '<a href="javascript:void(0)" onclick="$(\'#other-info\').toggle()"><small>补充其他信息</small></a>';
            modal += '</p>';
            modal += '<div id="other-info" style="display: none">';
            modal += '<div class="form-group">';
            modal += ' <label class="col-sm-2 control-label">邮箱</label>';
            modal += '<div class="col-sm-10">';
            modal += '<input type="text" class="form-control" id="email" placeholder="用于显示gravatar头像" maxlength="100">';
            modal += '</div>';
            modal += '</div>';
            modal += '<div class="form-group">';
            modal += '<label class="col-sm-2 control-label">网址</label>';
            modal += '<div class="col-sm-10">';
            modal += '<input type="text" class="form-control" id="website" placeholder="">';
            modal += '</div>';
            modal += '</div>';
            modal += '</div>';
        }

        modal += '</form>';
        modal += '</div>';
        modal += '<div class="modal-footer">';
        modal += '<button type="button" class="btn btn-default" data-dismiss="modal">关闭</button>';
        modal += ' <button type="button" class="btn btn-primary" id="comment-btn">提交</button>';
        modal += '</div>';
        modal += '</div>';
        modal += '</div>';
        modal += '</div>';
        $(modal).appendTo($('body'));
        var modal = $('#comment-modal');
        modal.on('show.bs.modal', function() {
            loadUserInfo();
            $.ajax({
                url: basePath + '/comment/needCaptcha',
                success: function(data) {
                    if (data) {
                    	$("#captchaContainer").show();
                    }else{
                    	$("#captchaContainer").hide();
                    }
                }
            })
        });
        
        $("#captcha-img").click(function(){
        	$(this).attr('src',basePath+'/captcha?time='+$.now());
        });
        
        modal.on('hidden.bs.modal', function() {
        	editor.clear();
            $("#comment-error-tip").html('').hide();
            moduleId = undefined;
            parentId = undefined;
            moduleType = undefined;
            commentFunction = undefined;
        });
        	$("#comment-btn").click(
                function() {
                    var me = $(this)
                    var comment = {};
                    comment.content = editor.get();
                    comment.website = $("#website").val();
                    comment.email = $("#email").val();
                    comment.nickname = $("#nickname").val();
                    if (parentId) {
                        comment.parent = {
                            id: parentId
                        };
                    }
                    $.ajax({
                        type: "post",
                        url: actPath + '/'+moduleType+'/' + moduleId + '/addComment?validateCode='+$("#comment-captcha").val(),
                        contentType: "application/json",
                        data: JSON.stringify(comment),
                        success: function(data) {
                            if (data.success) {
                            	 storeUserInfo(comment.nickname,
                                         comment.email, comment.website);
                            	 var check = data.data.status == 'CHECK';
                            	 if(!check && commentFunction){
                            		 commentFunction();
                            	 }
                                     $("#comment-modal").modal('hide');
                                	if (check) {
                                        bootbox.alert('评论将会在审核通过后显示');
                                        return;
                                    }
                               
                            } else {
                                $("#comment-error-tip").html(data.message)
                                    .show();
                            }
                        },
                        complete: function() {
                        	$("#captcha-img").attr('src',basePath+'/captcha?time='+$.now());
                            me.prop("disabled", false);
                        }
                    });

                });
        
        
        var conversationsModal = '<div class="modal " id="conversationsModal" tabindex="-1" role="dialog" >';
        conversationsModal += '<div class="modal-dialog" role="document">';
        conversationsModal += '<div class="modal-content">';
        conversationsModal += '<div class="modal-header">';
        conversationsModal += '<h4 class="modal-title">对话</h4>';
        conversationsModal += '</div>';
        conversationsModal += '<div class="modal-body" id="conversationsBody">';
        conversationsModal += '<div class="tip"></div>';
        conversationsModal += '</div>';
        conversationsModal += '<div class="modal-footer">';
        conversationsModal += '<button type="button" class="btn btn-default" data-dismiss="modal">关闭</button>';
        conversationsModal += '</div>';
        conversationsModal += '</div>';
        conversationsModal += '</div>';
        conversationsModal += '</div>';
        $(conversationsModal).appendTo($('body'));
        var conversationsModal = $("#conversationsModal");
        
        
        var queryConversations = function(id, moduleId,moduleType) {
            $.get(actPath + '/'+moduleType+'/' + moduleId + '/comment/' + id +
                    '/conversations', {},
                    function(data) {
                        if (!data.success) {
                            bootbox.alert(data.message);
                            return;
                        }
                        data = data.data;
                        var html = '';
                        for (var i = 0; i < data.length; i++) {
                            var c = data[i];
                            var p = i == 0 ? null : data[i - 1];
                            html += '<div class="media">';
                            html += '<div class="media-left">';
                            html += '<img class="media-object"  src="' +
                                getAvatar(c) +
                                '" data-holder-rendered="true" style="width: 32px; height: 32px;">';
                            html += '</div>';
                            html += '<div class="media-body"  >';
                            var username = getUsername(c);
                            var user = '<strong>' + username +
                                '</strong>';
                            var p_username = getUsername(p);
                            html += '<h5 class="media-heading">' + user +
                                '</h5>';
                            if (p) {
                                var pnickname = getUsername(p);
                                html += '<small style="margin-right:10px">回复' +
                                    pnickname + ':</small>';
                            }
                            html += '<div class="media-content">'
                            html += c.content;
                            html += '</div>';
                            html += '<h5>' +
                                new Date(c.commentDate)
                                .format('yyyy-mm-dd HH:MM') +
                                '&nbsp;&nbsp;&nbsp;</h5>';
                            html += '</div>';
                            html += '</div>';
                        }
                        $("#conversationsBody").html(html);
                        conversationsModal.modal('show');
                    });
        }
        
        var checkComment = function(id,callback){
        	 bootbox.confirm("确定要审核通过吗？", function(result) {
                 if (result) {
                     $.ajax({
                         type: "post",
                         url: basePath + "/mgr/comment/check?id=" + id,
                         data: {
                             id: id
                         },
                         xhrFields: {
                             withCredentials: true
                         },
                         crossDomain: true,
                         success: function(data) {
                            if(callback){
                            	callback();
                            }
                         },
                         complete: function() {}
                     });
                 }
             });
        }
        
        var removeComment =  function(id, callback) {
            bootbox.confirm(
                    "确定要删除该评论吗？",
                    function(result) {
                        if (result) {
                            $.ajax({
                                type: "post",
                                url: basePath +
                                    "/mgr/comment/delete?id=" +
                                    id,
                                contentType: "application/json",
                                data: {},
                                xhrFields: {
                                    withCredentials: true
                                },
                                crossDomain: true,
                                success: function(data) {
                                   if(callback){
                                	   callback();
                                   }
                                },
                                complete: function() {}
                            });
                        }
                    });
            }
        
        var banComment =  function(id, callback) {
            bootbox.confirm(
                    "确定要禁止该ip评论吗？",
                    function(result) {
                        if (result) {
                            $.ajax({
                                type: "post",
                                url: basePath +
                                    "/mgr/comment/ban?id=" +
                                    id,
                                contentType: "application/json",
                                data: {},
                                xhrFields: {
                                    withCredentials: true
                                },
                                crossDomain: true,
                                success: function(data) {
                                   if(callback){
                                	   callback();
                                   }
                                },
                                complete: function() {}
                            });
                        }
                    });
            }

        var loadUserInfo = function() {
            var name = '';
            var email = '';
            var website = '';
            if (window.localStorage) {
                if (localStorage.commentName) {
                    name = localStorage.commentName;
                }
                if (localStorage.commentEmail) {
                    email = localStorage.commentEmail;
                }
                if (localStorage.commentWebsite) {
                    website = localStorage.commentWebsite;
                }
            }
            $("#nickname").val(name);
            $("#email").val(email);
            $("#website").val(website);
        }
        var storeUserInfo = function(name, email, website) {
            if (window.localStorage) {
                if (name && name != '')
                    localStorage.commentName = name;
                else
                    localStorage.commentName = "";
                if (email && email != '')
                    localStorage.commentEmail = email;
                else
                    localStorage.commentEmail = "";
                if (website && website != '')
                    localStorage.commentWebsite = website;
                else
                    localStorage.commentWebsite = "";
            }
        }
        var getAvatar = function(c) {
            if (c.gravatar) {
                return config.gravatarPrefix + c.gravatar;
            }
            return basePath + '/static/img/guest.png';
        }

        var getUsername = function(c) {
            if (c == null || !c) {
                return '';
            }
            var username = '';
            if (c.admin) {
                username = '<span class="glyphicon glyphicon-user" style="color:red"  title="管理员"></span>&nbsp;' +
                    c.nickname
            } else {
                username = c.nickname
            }
            return username;
        }
        var isLogin = config.isLogin;
        var loadComment = function(config) {
            var pageSize = config.pageSize;
            if (!pageSize) {
                pageSize = 10;
            }
            var page = config.page;
            if (!page || page < 1) {
                page = 1;
            }
            var c = config.container;
            c.html('<img src="'+basePath+'/static/img/loading.gif" class="img-responsive center-block"/>')
            $.get(actPath + '/data/评论', {
                    moduleType: config.moduleType,
                    moduleId: config.moduleId,
                    currentPage: page,
                    pageSize: pageSize,
                    asc: config.asc
                },
                function(data) {
                    if (data.success) {
                        var page = data.data.data;
                        config.pageSize = page.param.pageSize;
                        config.currentPage = page.param.currentPage;
                        var html = '';
                        if (page.datas.length > 0) {
                            for (var i = 0; i < page.datas.length; i++) {
                                var data = page.datas[i];
                                html += '<div class="media">';
                                html += '<div class="media-left">';
                                if(data.admin){
                                	 html += '<a href="javascript:void(0)"> <img class="media-object" src="' +
                                     getAvatar(data) +
                                     '" style="width:24px;height:24px"></a>';
                                }else{
                                	var website = data.website;
                                    if(website){
                                    	 html += '<a href="'+website+'" target="_blank" rel="external nofollow"> <img class="media-object" src="' +
                                         getAvatar(data) +
                                         '" style="width:24px;height:24px"></a>';
                                    }else{
                                    	html += '<a href="javascript:void(0)"> <img class="media-object" src="' +
                                        getAvatar(data) +
                                        '" style="width:24px;height:24px"></a>';
                                    }
                                }
                                html += '</div>';
                                html += '<div class="media-body"  >';
                                var time = new Date(data.commentDate)
                                    .format('yyyy-mm HH:MM');
                                html += '<h6 class="media-heading">' +
                                    getUsername(data) + '</h6>';
                                if (data.parent) {
                                    var pnickname = getUsername(data.parent);
                                    html += '<small style="margin-right:10px">回复' +
                                        pnickname + ':</small>';
                                }
                                html += '<div class="media-content">';
                                html += data.content;
                                html += '</div>'
                                html += '<p><small>' + time +
                                    '</small>';
                                if (isLogin) {
                                	if(!data.admin && !data.ban){
                                    	html += '<a href="javascript:void(0)" data-ban data-moduletype="'+config.moduleType+'" data-moduleId="'+config.moduleId+'" data-comment="'+data.id+'" style="margin-left:10px"><span class="glyphicon glyphicon-remove-circle" aria-hidden="true"></span></a>';
                                	}
                                    html += '<a href="javascript:void(0)" data-del data-moduletype="'+config.moduleType+'" data-moduleId="'+config.moduleId+'" data-comment="'+data.id+'" style="margin-left:10px"><span class="glyphicon glyphicon-remove" aria-hidden="true"></span></a>';
                                }
                                if (data.status == 'CHECK') {
                                    html += '<a href="javascript:void(0)" data-check data-moduletype="'+config.moduleType+'" data-moduleId="'+config.moduleId+'" data-comment="'+data.id+'"  style="margin-left:10px"><span class="glyphicon glyphicon-ok" aria-hidden="true"></span></a>';
                                } else {
                                	if(config.allowComment || isLogin){
                                		html += '<a href="javascript:void(0)" data-reply data-moduletype="'+config.moduleType+'" data-moduleId="'+config.moduleId+'" data-comment="'+data.id+'"   style="margin-left:10px"><span class="glyphicon glyphicon-comment" aria-hidden="true"></span></a>';
                                	}
                                    if (data.parent) {
                                        html += '<a href="javascript:void(0)" data-conversations data-moduletype="'+config.moduleType+'" data-moduleId="'+config.moduleId+'" data-comment="'+data.id+'" style="margin-left:10px"><span class="glyphicon glyphicon-eye-open" aria-hidden="true"></span></a>';
                                    }
                                }
                                html += '</p>';
                                html += '</div>';
                                html += '</div>';
                            }
                        }
                        if (page.totalPage > 1) {
                            html += '<nav>';
                            html += '<ul class="pagination">';
                            html += '<li><a href="javascript:void(0)" data-page="1">«</a></li>';
                            for (var j = page.listbegin; j < page.listend; j++) {
                                if (j == page.currentPage) {
                                    html += '<li class="active"><a href="javascript:void(0)" >' +j + '</a></li>';
                                } else {
                                    html += '<li><a href="javascript:void(0)" data-page="'+j+'">'+j+'</a></li>';
                                }
                            }
                            html += '<li><a href="javascript:void(0)" data-page="'+page.totalPage+'">»</a></li>';
                            html += '</ul>';
                            html += '</nav>';
                        }
                        c.html(html);
                        var afterLoad = config.afterLoad;
                        if(afterLoad){
                        	afterLoad(page);
                        }
                    } else {
                        bootbox.alert(data.message);
                    }
                });
        }
        
        var commentConfig;
        
        $.ajax({
        	
        	url : basePath + '/comment/config',
        	async : false,
        	success:function(data){
        		
        		if(data.success){
        			commentConfig = data.data;
        		}
        	}
        	
        });
        
        var editor ;
        
        loadCSS = function(href) {

        	  var cssLink = $("<link>");
        	  $("head").append(cssLink); 

        	  cssLink.attr({
        	    rel:  "stylesheet",
        	    type: "text/css",
        	    href: href
        	  });

        };
        
        if(commentConfig.editor == 'HTML'){
        	if(config.isLogin){
        		loadCSS(basePath + '/static/summernote/dist/summernote.css');
            	$.getScript(basePath + "/static/summernote/dist/summernote.min.js" ,function(){
            		 $.getScript(basePath + "/static/summernote/dist/lang/summernote-zh-CN.min.js",function(){
            			 
            			 $('#content').summernote({
            					lang: 'zh-CN',
            					height:270,
            					focus:true,
            					toolbar:[
            						['style', ['style']],
            			            ['font', ['bold', 'underline', 'clear']],
            			            ['fontname', ['fontname']],
            			            ['color', ['color']],
            			            ['para', ['ul', 'ol', 'paragraph']],
            			            ['insert', ['link', 'video']],
            			            ['view', ['fullscreen', 'codeview']]
            					]
            				});
            			 
            			 editor = {
            					 
            					 get : function(){
            						 return $("#content").summernote('code');
            					 },
            					 clear : function(){
            						 $("#content").summernote('code','');
            					 }
            			 }
            			 
            		 });
            	})
        	} else {
        		editor = {
            			
                		get:function(){
                			return   $("#content").val();
                		}	,
                		clear:function(){
                			$("#content").val('');
                		}
                		
                	}
        	}
        } else {
        	
        	editor = {
        			
        		get:function(){
        			return   $("#content").val();
        		}	,
        		clear:function(){
        			$("#content").val('');
        		}
        		
        	}
        	
        }
        
        
        var cache = [];
        return {

            renderComment: function(config) {
                loadComment(config);
                var c = config.container;
                for(var i=0;i<cache.length;i++){
                	if(cache[i].is(c)){
                		return ;
                	}
                }
                c.on("click","[data-page]",function(){
                	config.page = parseInt($(this).attr('data-page'));
                	loadComment(config);
            	}); 
                c.on('click',"[data-conversations]",function(){
                	queryConversations($(this).data('comment'),$(this).data('moduleid'),$(this).data('moduletype'))
                });
                c.on('click',"[data-del]",function(){
                	removeComment($(this).data('comment'),function(){
                		loadComment(config);
                	});
                });
                c.on('click',"[data-check]",function(){
                	checkComment($(this).data('comment'),function(){
                		loadComment(config);
                	});
                });
                c.on('click','[data-ban]',function(){
                	banComment($(this).data('comment'),function(){
                		loadComment(config);
                	});
                });
                
                c.on('click',"[data-reply]",function(){
                	parentId = $(this).data('comment');
                	moduleId = $(this).data('moduleid');
                	moduleType = $(this).data('moduletype');
                	commentFunction = function(){
                		loadComment(config);
                	}
                	modal.modal('show');
                });
                cache.push(c);
            },
            
            doComment:function(_moduleId,_moduleType,fun){
            	moduleId=_moduleId;
            	moduleType=_moduleType;
            	if(fun){
            		commentFunction = fun;
            	}
            	modal.modal('show');
            }

        }
    })(config);