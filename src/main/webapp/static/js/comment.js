var max = 3;
var cp = 1;
var login = $("#login").val() == 'true';
var flag = false;
var asc = false;
var config;
$.ajax({
	type : "get",
	url : rootPath + '/comment/config',
	data : {},
	async : false,
	success : function(data) {
		if (data.success) {
			config = data.data;
		} else {
			console.log("查询评论配置异常:" + data.message);
		}
	},
	complete : function() {
	}
});
// 评论表单html

function getCommentFormHtml() {
	var html = '';
	html += '<div id="comments-content-container" style="margin-bottom:10px"></div>';
	html += '<form role="form">';
	if (!login) {
		var user = loadUserinfo();
		html += '<div class="form-group">';
		html += '<label>昵称(必须)</label>';
		html += '<input type="text" class="form-control" id="comment-nickname" value="'
				+ user.name + '" placeholder="">';
		html += '</div>';
		html += '<div class="form-group">';
		html += '<label>邮箱(非必需，用于gravatar头像和管理员回复邮件通知)</label>';
		html += '<input type="email" class="form-control"  placeholder="" value="'
				+ user.email + '" id="comment-email">';
		html += '</div>';
	}
	html += '<div class="form-group">';
	html += '  <label >评论内容(必填)</label>';
	html += ' <textarea class="form-control" rows="8" id="comment-content"></textarea>';
	html += '</div>';
	html += '<button type="button" id="add-comment-btn" class="btn btn-default">评论</button>';
	html += '</form>';
	return html;
}

function getReplyFormHtml() {
	var reply_html = '<div id="reply-tip"></div>';
	reply_html += '<form role="form">';
	if (!login) {
		var user = loadUserinfo();
		reply_html += '<div class="form-group">';
		reply_html += '<label>昵称(必须)</label>';
		reply_html += '<input type="text" class="form-control" value="'
				+ user.name + '"  id="reply-nickname" placeholder="">';
		reply_html += '</div>';
		reply_html += '<div class="form-group">';
		reply_html += '<label>邮箱(非必需，用于gravatar头像和管理员回复邮件通知)</label>';
		reply_html += '<input type="email" class="form-control"  value="'
				+ user.email + '" placeholder="" id="reply-email">';
		reply_html += '</div>';
	}
	reply_html += '<div class="form-group">';
	reply_html += '  <label >评论内容(必填)</label>';
	reply_html += ' <textarea class="form-control" rows="8" id="reply-content"></textarea>';
	reply_html += '</div>';
	reply_html += '</form>';
	return reply_html;
}

$(document).ready(
		function() {
			$("#comments-container").html(getCommentFormHtml());
			queryComments(cp);
			$("#add-comment-btn").click(
					function() {
						if (flag) {
							return;
						}
						flag = true;
						$.ajax({
							type : "post",
							url : actPath + "/article/" + $("#articleId").val()
									+ "/addComment",
							data : JSON.stringify({
								"content" : $('#comment-content').val(),
								"email" : $("#comment-email").val(),
								"nickname" : $("#comment-nickname").val()
							}),
							dataType : "json",
							contentType : 'application/json',
							success : function(data) {
								if (data.success) {
									if (data.data.status == 'CHECK') {
										bootbox.alert("评论成功，将会在审核通过后显示");
									} else {
										bootbox.alert("评论成功");
										queryComments(cp);
										$("#oauthEmail").val(
												$("#comment-email").val());
									}
									storeUserinfo($("#comment-nickname").val(),
											$("#comment-email").val(), '')
								} else {
									bootbox.alert(data.message);
								}
							},
							complete : function() {
								flag = false;
							}
						});
					})
		});
function queryComments(page) {
	cp = page;
	$.ajax({
		type : 'get',
		url : actPath + '/data/评论',
		data : {
			"currentPage" : page,
			"article" : $("#articleId").val()
		},
		dataType : "json",
		contentType : 'application/json',
		beforeSend : function(xhr) {
			xhr.setRequestHeader("X-Fragment", encodeURI("评论"));
		},
		success : function(result) {
			if (!result.success) {
				bootbox.alert(result.message);
				return;
			} else {
				$("#comments-content-container").html(result.data);
			}
		}
	});
}
function toReply(parent) {
	bootbox.dialog({
		title : '回复一个评论',
		message : getReplyFormHtml(),
		buttons : {
			success : {
				label : "确定",
				className : "btn-success",
				callback : function(e) {
					if (flag) {
						return false;
					}
					var data = {};
					data.parent = {
						"id" : parent
					};
					data.email = $("#reply-email").val();
					data.nickname = $("#reply-nickname").val();
					data.content = $('#reply-content').val();
					$("#reply-tip").html('')
					flag = true;
					var sign = false;
					$.ajax({
						type : "post",
						url : actPath + "/article/" + $("#articleId").val()
								+ "/addComment",
						data : JSON.stringify(data),
						async : false,
						dataType : "json",
						contentType : 'application/json',
						success : function(data) {
							if (data.success) {
								queryComments(cp);
								sign = true;
								storeUserinfo($("#reply-nickname").val(), $(
										"#reply-email").val(), '')
							} else {
								$("#reply-tip").html(
										'<div class="alert alert-danger">'
												+ data.message + '</div>');
							}
						},
						complete : function() {
							flag = false;
						}
					});
					return sign;
				}
			}
		}
	});
}
function storeUserinfo(name, email, website) {
	if (window.localStorage) {
		if (name)
			localStorage.commentName = name;
		else
			localStorage.commentName = "";
		if (email)
			localStorage.commentEmail = email;
		else
			localStorage.commentEmail = "";
		if (website)
			localStorage.commentWebsite = website;
		else
			localStorage.commentWebsite = "";
	}
}

function loadUserinfo() {
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
	return {
		"name" : name,
		"email" : email,
		"website" : website
	};
}

function queryConversations(id) {
	$
			.get(
					actPath + '/article/' + $("#articleId").val() + '/comment/'
							+ id + '/conversations',
					{},
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
							html += '<div class="media" id="comment-'
									+ c.id
									+ '" data-p="'
									+ ((!c.parent || c.parent == null) ? ''
											: c.parent.id) + '">';
							html += '<a class="pull-left">';
							html += '<img class="media-object"  src="'
									+ getAvatar(c)
									+ '" data-holder-rendered="true" style="width: 64px; height: 64px;">';
							html += '</a>';
							html += '<div class="media-body">';
							var username = getUsername(c);
							var user = '<strong>' + username + '</strong>';
							var p_username = getUsername(p);
							var reply = (p == null || !p) ? ""
									: '<span class="glyphicon glyphicon-share-alt" aria-hidden="true"></span>&nbsp;&nbsp;'
											+ p_username;
							html += '<h5 class="media-heading">' + user
									+ '&nbsp;&nbsp;&nbsp;' + reply + '</h5>';
							html += c.content;
							html += '<h5>'
									+ new Date(c.commentDate)
											.format('yyyy-mm-dd HH:MM:ss')
									+ '&nbsp;&nbsp;&nbsp;</h5>';
							html += '</div>';
							html += '</div>';
						}
						if ($('#conversationsModal').length == 0) {
							var modal = '<div class="modal " id="conversationsModal" tabindex="-1" role="dialog" >';
							modal += '<div class="modal-dialog" role="document">';
							modal += '<div class="modal-content">';
							modal += '<div class="modal-header">';
							modal += '<h4 class="modal-title">对话</h4>';
							modal += '</div>';
							modal += '<div class="modal-body" id="conversationsBody">';
							modal += '<div class="tip"></div>';
							modal += '</div>';
							modal += '<div class="modal-footer">';
							modal += '<button type="button" class="btn btn-default" data-dismiss="modal">关闭</button>';
							modal += '</div>';
							modal += '</div>';
							modal += '</div>';
							modal += '</div>';
							$(modal).appendTo($('body'));
						}
						$('#conversationsBody').html(html);
						$('#conversationsModal').modal('show');
					});
}

function getAvatar(c) {
	if (c.gravatar) {
		return 'http://cn.gravatar.com/avatar/' + c.gravatar;
	}
	return rootPath + '/static/img/guest.png';
}

function getUsername(c) {
	if (c == null || !c) {
		return '';
	}
	var username = '';
	if (c.admin) {
		username = '<span class="glyphicon glyphicon-user" style="color:red" title="管理员"></span>&nbsp;'
				+ c.nickname
	} else {
		username = c.nickname
	}
	return username;
}

function removeComment(id) {
	bootbox.confirm("确定要删除吗？", function(result) {
		if (result) {
			$.ajax({
				type : "post",
				url : rootPath + "/mgr/comment/delete?id=" + id,
				contentType : "application/json",
				data : {},
				xhrFields : {
					withCredentials : true
				},
				crossDomain : true,
				success : function(data) {
					if (data.success) {
						queryComments(cp);
					} else {
						bootbox.alert(data.message);
					}
				},
				complete : function() {
				}
			});
		}
	});
}

function checkComment(id) {
	bootbox.confirm("确定要审核通过吗？", function(result) {
		if (result) {
			$.ajax({
				type : "post",
				url : rootPath + "/mgr/comment/check?id=" + id,
				data : {
					id : id
				},
				xhrFields : {
					withCredentials : true
				},
				crossDomain : true,
				success : function(data) {
					queryComments(cp);
				},
				complete : function() {
				}
			});
		}
	});
}