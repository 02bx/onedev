var gitplex = {};
String.prototype.escape = function() {
	return (this + '').replace(/[\\"']/g, '\\$&').replace(/\u0000/g, '\\0');
};
gitplex.server = {
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
				gitplex.server.form.dirtyChanged($(this));
			});
		},
		markClean: function($forms) {
			$forms.removeClass("dirty").each(function() {
				gitplex.server.form.dirtyChanged($(this));
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
					gitplex.server.form.markClean($forms);
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
						gitplex.server.form.dirtyChanged($(this));
					}
				});
				if ($form.find(".has-error").length != 0) {
					$form.addClass("dirty");
				}
				gitplex.server.form.dirtyChanged($form);
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
				gitplex.server.form.trackDirty(this);
			});
			
			$(document).on("elementReplaced", function(event, componentId) {
				var $component = $("#" + componentId);
				var $forms = $component.find("form");
				if ($component.is("form"))
					$forms = $forms.add($component);
				$forms.each(function() {
					gitplex.server.form.trackDirty(this);
				});
				var $form = $component.closest("form").not($component);
				if ($form.find(".dirty-aware").length != 0 || $form.hasClass("leave-confirm"))
					$form.trigger("checkform.areYouSure");
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
			var selector = "form.leave-confirm.dirty";
			var $dirtyForms = $container.find(selector).addBack(selector);
			if ($dirtyForms.length != 0) {
				if (confirm("There are unsaved changes, do you want to discard and continue?")) {
					gitplex.server.form.clearAutosavings($dirtyForms);
				} else {
					return false;
				}
			} else {
				return true;
			}
		},
		clearAutosavings: function($dirtyForms) {
			$dirtyForms.each(function() {
				var autosaveKey = $(this).data("autosaveKey");
				if (autosaveKey)
					localStorage.removeItem(autosaveKey);
			});
		},
		registerAutosaveKey: function($form, autosaveKey) {
			$form.data("autosaveKey", autosaveKey);
		}
	},
	setupAutoSize: function() {
		function doAutosize($textarea) {
			$textarea.each(function() {
				if ($(this).closest(".no-autosize").length == 0) {
					autosize($(this));
				}
			});
		}
		
		doAutosize($("textarea"));
		
		$(document).on("elementReplaced", function(event, componentId) {
			var $component = $("#" + componentId);
			var $textarea = $component.find("textarea");
			if ($component.is("textarea"))
				$textarea = $textarea.add($component);
			doAutosize($textarea);
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
				if (!$ajaxLoadingIndicator.is(":visible") && $(".ajax-indicator:visible").length == 0)
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
				gitplex.server.focus.doFocus($("#" + componentId));
			gitplex.server.focus.$components = null;
		},
		
		doFocus: function($containers) {
			var selector = ".has-error:visible:first .autofocus";
			var $inError = $containers.find(selector).addBack(selector);
			if ($inError.length != 0 && $inError.closest(".no-autofocus").length == 0) {
				$inError.focus();
			} else {
				selector = ".autofocus:visible";
				$containers.find(selector).addBack(selector).each(function() {
					var $this = $(this);
					if ($this.closest(".no-autofocus").length == 0) {
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
					gitplex.server.focus.focusOn(componentId);
					wicketSetFocusOnId(componentId);
				}
			}
			
			Wicket.Event.subscribe('/ajax/call/beforeSend', function() {
				gitplex.server.focus.$components = $();
			});
			Wicket.Event.subscribe('/ajax/call/complete', function() {
				if (gitplex.server.focus.$components != null)
					gitplex.server.focus.doFocus(gitplex.server.focus.$components);
			});

			gitplex.server.focus.doFocus($(document));

			$(document).on("elementReplaced", function(event, componentId) {
				if (gitplex.server.focus.$components != null)
					gitplex.server.focus.$components = gitplex.server.focus.$components.add("#" + componentId);
			});			
		},
		
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
	        feedback.css("left", ($(window).width()-feedback.outerWidth()) / 2);
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
	
	choiceFormatter: {
		formatSelection: function(choice) {
			return choice.id;
		},
		
		formatResult: function(choice) {
			return choice.id;
		},
		
		escapeMarkup: function(m) {
			return m;
		}
	},	
	
	setupWebsocketCallback: function() {
		Wicket.Event.subscribe("/websocket/message", function(jqEvent, message) {
			if (message == "RenderCallback")
				Wicket.WebSocket.send(message);
			else if (message == "ErrorMessage")
				$("#websocket-error").show();
		});
		Wicket.Event.subscribe("/websocket/open", function(jqEvent) {
			Wicket.WebSocket.send("ConnectCallback");
		});
	},

	/*
	 * View state represents scroll position and edit cursor. We restore view state
	 * when page is backed, or in some cases carry over the view state from one page 
	 * to another (such as scrolling the page to same position being viewed when edit 
	 * a source code)
	 */
	viewState: {
		getInnerMostAutoFit: function() {
			var $innerMost;
			var $autofits = $(".autofit:visible");
			while ($autofits.length != 0) {
				$innerMost = $autofits.first();
				$autofits = $innerMost.find(".autofit:visible");
			}
			return $innerMost;
		},
		getFromView: function() {
			var $innerMost = gitplex.server.viewState.getInnerMostAutoFit();
			if ($innerMost) {
				return $innerMost.triggerHandler("getViewState");
			} else {
				return undefined;
			}
		},
		setToView: function(viewState) {
			var $innerMost = gitplex.server.viewState.getInnerMostAutoFit();
			if ($innerMost) {
				$innerMost.triggerHandler("setViewState", viewState);
			}
		},
		getFromHistory: function() {
			if (history.state && history.state.viewState)
				return history.state.viewState;
			else
				return undefined;
		},
		setToHistory: function(viewState) {
			var state = history.state;
			if (!state)
				state = {};
			var newState = {viewState: viewState, data: state.data, visited: state.visited};
			history.replaceState(newState, '', window.location.href );
			gitplex.server.history.current = {
				state: newState,
				url: window.location.href
			};
		},
		getFromViewAndSetToHistory: function() {
			var viewState = gitplex.server.viewState.getFromView();
			if (viewState)
				gitplex.server.viewState.setToHistory(viewState);
		},
		getFromHistoryAndSetToView: function() {
			var viewState = gitplex.server.viewState.getFromHistory();
			if (viewState)
				gitplex.server.viewState.setToView(viewState);
		}
	},
	
	history: {
		getHashlessUrl: function(url) {
			var index = url.indexOf('#');
			if (index != -1)
				return url.substring(0, index);
			else
				return url;
		},
		init: function(callback) {
			// Use a timeout here solve the problem that Safari (and previous versions of Chrome) 
			// fires event "onpopstate" on initial page load and this causes the page to reload 
			// infinitely  
			setTimeout(function() {
				window.onpopstate = function(event) {
					if (gitplex.server.history.getHashlessUrl(window.location.href) 
							!= gitplex.server.history.getHashlessUrl(gitplex.server.history.current.url)) {
						if (gitplex.server.form.confirmLeave()) {
							if (!event.state || !event.state.data) {
								location.reload();
							} else {
								callback(event.state.data);
								gitplex.server.history.current = {
									state: event.state,
									url: window.location.href
								};
							}
						} else {
							/*
							 * In case we want to stay in the page, we should also re-push previous url 
							 * as url has already been changed when user hits back/forward button
							 */
							history.pushState(gitplex.server.history.current.state, '' , 
									gitplex.server.history.current.url);
						}
					} else {
						gitplex.server.viewState.getFromHistoryAndSetToView();
					}
				};
			}, 100);
			
			gitplex.server.history.current = {
				url: window.location.href 
			};
		},
		pushState: function(url, data) {
			var state = {data: data};
			gitplex.server.history.current = {state: state, url: url};
			history.pushState(state, '', url);
			
			// Let others have a chance to do something before marking the page as visited
			setTimeout(function() {
				gitplex.server.history.setVisited();
			}, 100);
		},
		
		/*
		 * visited flag is used to determine whether or not a page is newly visited 
		 * or loaded via back/forward button. If loaded via back/forward button we
		 * will not respect scroll flag (such as mark param in url) and let browser
		 * scroll to original position for better usage experience 
		 */
		setVisited: function() {
			var state = history.state;
			if (!state)
				state = {};
			if (!state.visited) {
				var newState = {viewState: state.viewState, data: state.data, visited: true};
				history.replaceState(newState, '', window.location.href);
				gitplex.server.history.current = {
					state: newState,
					url: window.location.href
				};
			}
		},
		isVisited: function() {
			return history.state != undefined && history.state.visited === true;
		},
	},
	isDevice: function() {
		var ua = navigator.userAgent.toLowerCase();
		return ua.indexOf("android") != -1 
				|| ua.indexOf("iphone") != -1 
				|| ua.indexOf("ipad") != -1 
				|| ua.indexOf("windows phone") != -1; 
	},
	isMac: function() {
		return navigator.userAgent.indexOf('Mac') != -1;		
	},
	mouseState: {
		pressed: false, 
		moved: false,
		track: function() {
			$(document).mousedown(function() { 
				gitplex.server.mouseState.pressed = true;
				gitplex.server.mouseState.moved = false;
			});
			$(document).mouseup(function() {
				gitplex.server.mouseState.pressed = false;
				gitplex.server.mouseState.moved = false;
			});	
			$(document).mousemove(function(e) {
				// IE fires mouse move event after mouse click sometimes, so we check 
				// if mouse is really moved here
				if (e.clientX != self.clientX || e.clientY != self.clientY) {
					gitplex.server.mouseState.moved = true;
					self.clientX = e.clientX;
					self.clientY = e.clientY;
				}
			});
			$(document).scroll(function() {
				gitplex.server.mouseState.moved = false;
			});
		}
	},
	
	onDomReady: function(autosaveKeyToClear) {
		gitplex.server.setupAjaxLoadingIndicator();
		gitplex.server.form.setupDirtyCheck();
		gitplex.server.focus.setupAutoFocus();
		gitplex.server.setupWebsocketCallback();
		gitplex.server.mouseState.track();
		
		if (autosaveKeyToClear)
			localStorage.removeItem(autosaveKeyToClear);
		
		$(document).keydown(function(e) {
			if (e.keyCode == 27)
				e.preventDefault();
		});
		
		window.onunload = function() {
			gitplex.server.form.clearAutosavings($("form.leave-confirm.dirty"));
		};
	},
	
	onWindowLoad: function() {
		gitplex.server.setupAutoSize();
		/*
		 * Browser will also issue resize event after window is loaded, but that is too late, 
		 * as getFromHistoryAndSetToView() must be happened after view size has been adjusted
		 */
		$(window).resize(); 
		gitplex.server.viewState.getFromHistoryAndSetToView();

		// Let others have a chance to do something before marking the page as visited
		setTimeout(function() {
			gitplex.server.history.setVisited();
		}, 100);
		
		/*
		 * Disable this as calling replaceState in beforeunload also affects 
		 * state of the page to be loaded
		 */
		/*
		$(window).on("beforeunload", function() {
			gitplex.server.viewState.getFromViewAndSetToHistory();	
		});
		*/
	}
};
