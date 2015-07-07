var gitplex = {
	selectDirectory: function(inputId, triggerId, path, append) {
		var input = $("#" + inputId);
		if (!append) {
			input.val(path);
			pmease.commons.dropdown.hide($("#" + triggerId).closest(".dropdown-panel")[0].id);
		} else {
			var value = input.val();
			if (value.match(/.*,\s*$/g) || $.trim(value).length == 0)
				input.val(value + path);
			else
				input.val(value + ", " + path);
		}
	},
	
	choiceFormatter: {
		repository: {
			formatSelection: function(repository) {
				return repository.name;
			},
			
			formatResult: function(repository) {
				return repository.name;
			},
			
			escapeMarkup: function(m) {
				return m;
			}
		}, 

		branch: {
			formatSelection: function(branch) {
				return branch.name;
			},
			
			formatResult: function(branch) {
				return branch.name;
			},
			
			escapeMarkup: function(m) {
				return m;
			}
		},

		team: {
			formatSelection: function(team) {
				return team.name;
			},
			
			formatResult: function(team) {
				return team.name;
			},
			
			escapeMarkup: function(m) {
				return m;
			}
		},
		
	},
	
	commitMessage: {
		toggle: function(toggle) {
			$(toggle).click(function(e) { 
				var $self = $(this); 
				$self.toggleClass('collapsed'); 
				$self.parent().siblings('.detailed-message').toggle(); 
			});
		}
	},
	
	utils: {
		/**
		 * Trigger a callback when the selected images are loaded:
		 * @param {String} selector
		 * @param {Function} callback
		 */
		 onImageLoad: function(selector, callback){
		    $(selector).each(function(){
		        if (this.complete || /*for IE 10-*/ $(this).height() > 0) {
		            callback.apply(this);
		        }
		        else {
		            $(this).on('load', function(){
		                callback.apply(this);
		            });
		        }
		    });
		}
	}, 
	
	comments: {
		position: function() {
			$("#comments-placeholder").children("table").each(function() {
				var $lineComments = $(this);
				var id = $lineComments.attr("id");
				var lineId = id.substring("comment-".length);
				var $line = $("#" + lineId);
				var $tr = $("<tr class='line comments'><td colspan='3'></td></tr>").insertAfter($line);
				$tr.children().append($lineComments);
				$line.find("a.add-comment").hide();
			});
		},
		beforeAdd: function(index) {
			var $line = $("#diffline-" + index); 

			var $tr = $("<tr class='line comments'><td colspan='3'></td></tr>").insertAfter($line);
			var $addComment = $("#add-comment");
			var $commentsRow = $addComment.closest("tr.line.comments");
			$addComment.find("textarea").val("");
			$tr.children().append($addComment);
			$commentsRow.prev().find("a.add-comment").show();
			$commentsRow.remove();
			$addComment.find("textarea").focus();
			pmease.commons.form.trackDirty($addComment);
			$line.find("a.add-comment").hide();
		},
		afterAdd: function(index) {
			$("#comments-placeholder").append($("#add-comment"));
			var $line = $("#diffline-" + index);
			$line.next().find("td").append($("#comment-diffline-" + index));
			$("#add-comment").areYouSure({"silent": "true"});
		},
		cancelAdd: function(index) {
			$("#comments-placeholder").append($("#add-comment"));
			var $line = $("#diffline-" + index);
			$line.next().remove(); 
			$line.find("a.add-comment").show();
			$("#add-comment").areYouSure({"silent": "true"});
		},
		afterRemove: function(index) {
			$("#comment-diffline-" + index).closest("tr").remove();
			$("#diffline-" + index).find("a.add-comment").show();
		}
	},
	
	expandable: {
		mouseDown: false,
		check: function() {
			if (gitplex.expandable.getScrollTop && !gitplex.expandable.mouseDown && $(".CodeMirror-fullscreen").length == 0) {
				var scrollTop = gitplex.expandable.getScrollTop();
				var $hideable = $(".hideable");
				if ($hideable.is(":visible")) {
					var height = 0;
					var topHeight = 0;
					$hideable.each(function() {
						if ($(this).hasClass("top"))
							topHeight += $(this).outerHeight();
						height += $(this).outerHeight();
					});
					if (scrollTop>height+10) {
						$hideable.hide();
						$(window).resize();
						gitplex.expandable.setScrollTop(scrollTop-topHeight);
					}
				} else if (scrollTop < 5) {
					$hideable.show();
					$(window).resize();
				}
			}
			setTimeout(gitplex.expandable.check, 100);
		}
	}
};

$(document).ready(function() {
	$(window).load(function() {
		$(document).mousedown(function() { 
			gitplex.expandable.mouseDown = true;
		});
		$(document).mouseup(function() {
			gitplex.expandable.mouseDown = false;
		});		
		gitplex.expandable.check();
	});
});
