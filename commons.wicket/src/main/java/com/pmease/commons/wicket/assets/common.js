/**************************************************************
 * Define common javascript functions and setups here.
 * 
 **************************************************************/

var pmease = {};
String.prototype.escape = function() {
	return (this + '').replace(/[\\"']/g, '\\$&').replace(/\u0000/g, '\\0');
};
pmease.commons = {
	setupCollapse: function(triggerId, targetId) {
		var trigger = $("#" + triggerId);
		var target = $("#" + targetId);

		// This script can still be called if CollapseBehavior is added to a 
		// a component enclosed in an invisible wicket:enclosure. So we 
		// should check if relevant element exists.
		if (!trigger[0] || !target[0])
			return;
		
		target[0].trigger = trigger[0];

		target.on("shown.bs.collapse hidden.bs.collapse", function() {
			var $floating = target.closest(".floating");
			if ($floating.length != 0) {
				var borderTop = $(window).scrollTop();
				var borderBottom = borderTop + $(window).height();
				var borderLeft = $(window).scrollLeft();
				var borderRight = borderLeft + $(window).width();

				var left = $floating.position().left;
				var top = $floating.position().top;
				var width = $floating.outerWidth();
				var height = $floating.outerHeight();
				
				if (left < borderLeft || left + width > borderRight 
						|| top < borderTop || top + height > borderBottom) {
					if ($floating.data("alignment"))
						$floating.align($floating.data("alignment"));
				}
			}
			
		});
		trigger.click(function() {
			if (target[0].collapsibleIds == undefined) {
				if (!target.hasClass("in")) {
					target.collapse("show");
					$(target[0].trigger).removeClass("collapsed");
				} else {
					target.collapse("hide");
					$(target[0].trigger).addClass("collapsed");
				}
			} else if (!target.hasClass("in")) {
				for (var i in target[0].collapsibleIds) {
					var collapsible = $("#" + target[0].collapsibleIds[i]);
					if (collapsible.hasClass("in")) {
						collapsible.collapse("hide");
						$(collapsible[0].trigger).addClass("collapsed");
					}
				}
				target.collapse("show");
				$(target[0].trigger).removeClass("collapsed");
			}
		});
	},
	
	setupAccordion: function(accordionId) {
		var accordion = $("#" + accordionId);
		var collapsibleIds = new Array();
		accordion.find(".collapse:not(#" + accordionId + " .collapse .collapse, #" + accordionId + " .accordion .collapse)").each(function() {
			collapsibleIds.push(this.id);
		});
		if (collapsibleIds[0]) {
			var collapsible = $("#" + collapsibleIds[0]);
			collapsible.removeClass("collapse");
			collapsible.addClass("in");
		}
		for (var i in collapsibleIds) {
			var collapsible = $("#" + collapsibleIds[i]);
			if (i == 0) {
				$(collapsible[0].trigger).removeClass("collapsed");
				collapsible.removeClass("collapse");
				collapsible.addClass("in");
			} else {
				$(collapsible[0].trigger).addClass("collapsed");
			}
			collapsible[0].collapsibleIds = collapsibleIds;
		}
	},
	
	editable: {
		adjustReflectionEditor: function(editorId) {
			var editor = $("#" + editorId);
			var input = editor.find(".value>input[type=checkbox]");
			input.parent().prev("label.name").addClass("pull-left");
			input.after("<div style='clear:both;'/>")
			
			input = editor.find(".value>div>input[type=checkbox]");
			input.parent().parent().prev("label.name").addClass("pull-left");
			input.after("<div style='clear:both;'/>")
		}
	},
	
	form: {
		/*
		 * This function can be called to mark enclosing form of specified element dirty. It should be
		 * called if underlying data has been changed but no form fields are updated, for instance 
		 * when sorting the elements inside a form. 
		 */
		markDirty: function($forms) {
			$forms.addClass("dirty").each(function() {
				pmease.commons.form.dirtyChanged($(this));
			});
		},
		markClean: function($forms) {
			$forms.removeClass("dirty").each(function() {
				pmease.commons.form.dirtyChanged($(this));
			});
		},
		removeDirty: function(triggerId, $forms) {
			$(function() {
				var $trigger = $("#" + triggerId);
				
				var previousClick;

				var handlers = $._data($trigger[0], 'events').click;

				$.each(handlers, function(i,f) {
					previousClick = f.handler; 
					return false; 
				});
				
				$trigger.unbind('click');

				$trigger.click(function(event){
					pmease.commons.form.markClean($forms);
					previousClick(event);
				});
			});
		},
		trackDirty: function(form) {
			var $form = $(form);
			if ($form.find(".dirty-aware").length != 0 || $form.hasClass("leave-confirm")) {
				$form.areYouSure({
					"silent": !$form.hasClass("leave-confirm"),
					"addRemoveFieldsMarksDirty": true,
					change: function() {
						pmease.commons.form.dirtyChanged($(this));
					}
				});
				if ($form.find(".has-error").length != 0) {
					$form.addClass("dirty");
				}
				pmease.commons.form.dirtyChanged($form);
			}
		},
		dirtyChanged: function($form) {
			$dirtyAware = $form.find(".dirty-aware");
			if ($dirtyAware.length != 0) {
				if ($form.hasClass("dirty")) {
					$dirtyAware.removeAttr("disabled");
				} else {
					$dirtyAware.attr("disabled", "disabled");
				}
			}
		},
		setupDirtyCheck: function() {
			$("form").each(function() {
				pmease.commons.form.trackDirty(this);
			});
			
			$(document).on("elementReplaced", function(event, componentId) {
				var $component = $("#" + componentId);
				var $forms = $component.find("form");
				if ($component.is("form"))
					$forms = $forms.add($component);
				$forms.each(function() {
					pmease.commons.form.trackDirty(this);
				});
				$component.closest("form.ays-inited").not($component).trigger("checkform.areYouSure");
			});
			
			if (Wicket && Wicket.Ajax) {
				var processAjaxResponse = Wicket.Ajax.Call.prototype.processAjaxResponse;
				Wicket.Ajax.Call.prototype.processAjaxResponse = function (data, textStatus, jqXHR, context) {
					if (jqXHR.readyState === 4) {
						var redirectUrl;
						try {
							redirectUrl = jqXHR.getResponseHeader('Ajax-Location');
						} catch (ignore) { // might happen in older mozilla
						}

						if (typeof(redirectUrl) !== "undefined" && redirectUrl !== null && redirectUrl !== "") {
							$("form.leave-confirm").removeClass("dirty");
						}
					}
					processAjaxResponse.call(this, data, textStatus, jqXHR, context);					
				}
			}
		},
		confirmLeave: function(containerId) {
			var $container;
			if (containerId)
				$container = $("#" + containerId);
			else
				$container = $(document);
			var dirty = "form.leave-confirm.dirty";
			if ($container.is(dirty) || $container.find(dirty).length != 0) 
				return confirm("There are unsaved changes, do you want to discard and continue?");
			else
				return true;
		}
	},
	setupAutoSize: function() {
		autosize($("textarea"));
		$(document).on("elementReplaced", function(event, componentId) {
			var $component = $("#" + componentId);
			var $textarea = $component.find("textarea");
			if ($component.is("textarea"))
				$textarea = $textarea.add($component);
			autosize($textarea);
		});
	},	
	setupAjaxLoadingIndicator: function() {
		$("#ajax-loading-overlay").click(function(e) {
			e.stopPropagation();
		});

		Wicket.Event.subscribe('/ajax/call/beforeSend', function() {
			var $ajaxLoadingIndicator = $("#ajax-loading-indicator");
			if ($ajaxLoadingIndicator[0].timer)
				clearTimeout($ajaxLoadingIndicator[0].timer);
			$ajaxLoadingIndicator[0].timer = setTimeout(function() {
				if (!$ajaxLoadingIndicator.is(":visible"))
					$ajaxLoadingIndicator.show();
			}, 2000);		
		});
		
		Wicket.Event.subscribe('/ajax/call/complete', function() {
			var $ajaxLoadingIndicator = $("#ajax-loading-indicator");
			if ($ajaxLoadingIndicator[0].timer) {
				clearTimeout($ajaxLoadingIndicator[0].timer);
				$ajaxLoadingIndicator[0].timer = null;
			}
			$ajaxLoadingIndicator.hide();
		});
	}, 
		
	focus: {
		$components: null,
		
		focusOn: function(componentId) {
			if (componentId)
				pmease.commons.focus.doFocus($("#" + componentId));
			pmease.commons.focus.$components = null;
		},
		
		doFocus: function($containers) {
			var $inError = $containers.find(".has-error:first .focusable");
			if ($inError.length != 0) {
				$inError.focus();
			} else {
				$containers.find(".focusable:visible").each(function() {
					var $this = $(this);
					if ($this.parents(".nofocus").length == 0) {
						$this.focus();
						return false;
					}
				});
			}
		},
		
		setupAutoFocus: function() {
			if (typeof(Wicket) != "undefined" && typeof(Wicket.Focus) != "undefined") {
				var wicketSetFocusOnId = Wicket.Focus.setFocusOnId;
				Wicket.Focus.setFocusOnId = function(componentId) {
					pmease.commons.focus.focusOn(componentId);
					wicketSetFocusOnId(componentId);
				}
			}
			
			Wicket.Event.subscribe('/ajax/call/beforeSend', function() {
				pmease.commons.focus.$components = $();
			});
			Wicket.Event.subscribe('/ajax/call/complete', function() {
				if (pmease.commons.focus.$components != null)
					pmease.commons.focus.doFocus(pmease.commons.focus.$components);
			});

			pmease.commons.focus.doFocus($(document));

			$(document).on("elementReplaced", function(event, componentId) {
				if (pmease.commons.focus.$components != null)
					pmease.commons.focus.$components = pmease.commons.focus.$components.add("#" + componentId);
			});			
		},
		
	},

	// Disable specified button if value of specified input is blank 
	disableIfBlank: function(inputId, buttonId) {
		var $input = $("#" + inputId);
		$input.bind("input propertychange keyup", function() {
			var value = $(this).val();
			var $button = $("#" + buttonId);
			if (value.trim().length != 0)
				$button.removeAttr("disabled");
			else
				$button.attr("disabled", "disabled");
		});
		$input.trigger("input");
	},
	
	autoHeight: function(targetSelector, bottomOffset) {
		var adjustHeight = function() {
			$(targetSelector).css("max-height", $(document).scrollTop() + $(window).height()
					- $(targetSelector).offset().top - bottomOffset + "px");
		};
		adjustHeight();
		$(window).resize(adjustHeight);
		$(window).scroll(adjustHeight);
	},
	
	showSessionFeedback: function() {
		if ($("#session-feedback li").length != 0) {
			var feedback = $("#session-feedback");
	        var x = ($(window).width() - feedback.outerWidth()) / 2;
	        feedback.css("left", x+$(window).scrollLeft());
			feedback.css("top", $(window).scrollTop());
			feedback.slideDown("slow");
			
			var body = $("body");
			if (body[0].hideCatchAllFeedbackTimer) {
				clearTimeout(body[0].hideCatchAllFeedbackTimer);
			}
			body[0].hideCatchAllFeedbackTimer = setTimeout(function() {
				$("#session-feedback").slideUp();
			}, 5000);
		}
	},
	
	backToTop: function(backToTop) {
		var $backToTop = $(backToTop);
        $backToTop.hide();
        $(window).scroll(function(){
        	if ($(window).scrollTop()>500)
        		$backToTop.fadeIn(1000);
        	else
        		$backToTop.fadeOut(1000);
        });
        $backToTop.click(function(){
        	$("body, html").animate({scrollTop:0}, 700);
        	return false;
        });
	},
	
	choiceFormatter: {
		id: {
			formatSelection: function(choice) {
				return choice.id;
			},
			
			formatResult: function(choice) {
				return choice.id;
			},
			
			escapeMarkup: function(m) {
				return m;
			}
		}
	},	
	
	websocket: {
		setupCallback: function() {
			Wicket.Event.subscribe("/websocket/message", function(jqEvent, message) {
				var json = JSON.parse(message);
				if (json.type == "RenderCallback")
					Wicket.WebSocket.send(message);
				else if (json.type == "ErrorMessage")
					$("#websocket-error").show();
			});
		},
	},

	history: {
		init: function(callback) {
			pmease.commons.history.urlWithoutHash = location.pathname+(location.search?location.search:"");
			
			// Use a timeout here solve the problem that Safari (and previous versions of Chrome) 
			// fires event "onpopstate" on initial page load and this causes the page to reload 
			// infinitely  
			setTimeout(function() {
				window.onpopstate = function(event) {
					var currentUrlWithoutHash = location.pathname+(location.search?location.search:"");
					if (currentUrlWithoutHash != pmease.commons.history.urlWithoutHash) {
						if (pmease.commons.form.confirmLeave()) {
							if (!event.state || !event.state.data) 
								location.reload();
							else 
								callback(event.state.data);
							pmease.commons.history.current = {
								state: event.state,
								url: window.location.href
							};
						} else {
							history.pushState(pmease.commons.history.current.state, '' , 
									pmease.commons.history.current.url);
						}
					}
					pmease.commons.history.urlWithoutHash = currentUrlWithoutHash;
				};
			}, 100);
			
			pmease.commons.history.current = {
				url: window.location.href	
			};
		},
		pushState: function(url, data) {
			var state = {data: data};
			pmease.commons.history.current = {state: state, url: url};
			history.pushState(state, '', url);
			pmease.commons.history.urlWithoutHash = location.pathname+(location.search?location.search:'');
		},
		replaceState: function(data, url) {
			var state = {data: data};
			pmease.commons.history.current = {state: state, url: url};
			history.replaceState(state, '', url);
			pmease.commons.history.urlWithoutHash = location.pathname+(location.search?location.search:'');
		},
		setViewState: function(viewState) {
			var state = history.state;
			if (!state)
				state = {};
			var newState = {viewState: viewState, data: state.data};
			history.replaceState(newState, '', window.location.href );
			pmease.commons.history.current = {
				state: newState,
				url: window.location.href
			};
		}, 
		getViewState: function() {
			if (history.state && history.state.viewState)
				return history.state.viewState;
			else
				return undefined;
		}
	},
	isDevice: function() {
		var ua = navigator.userAgent.toLowerCase();
		return ua.indexOf("android") != -1 
				|| ua.indexOf("iphone") != -1 
				|| ua.indexOf("ipad") != -1 
				|| ua.indexOf("windows phone") != -1; 
	}
};

$(function() {
	pmease.commons.setupAutoSize();
	pmease.commons.setupAjaxLoadingIndicator();
	pmease.commons.form.setupDirtyCheck();
	pmease.commons.focus.setupAutoFocus();
	pmease.commons.websocket.setupCallback();
});
