import {
  Component,
  ComponentFactoryResolver,
  Directive,
  Inject,
  InjectionToken,
  Input,
  OnInit,
  ViewChild,
  ViewContainerRef
} from '@angular/core';
import {PortofinoService} from "./portofino.service";
import {AuthenticationService} from "./security/authentication.service";
import {TranslateService} from "@ngx-translate/core";
import {TRANSLATIONS_EN} from "./i18n/en";
import {TRANSLATIONS_IT} from "./i18n/it";
import {NAVIGATION_COMPONENT, NavigationComponent} from "./page";
import {NavigationDirective} from "./content.directive";

export const TOOLBAR_COMPONENT = new InjectionToken('Toolbar Component');

@Directive({
  selector: '[portofino-toolbar]'
})
export class ToolbarDirective {
  constructor(public viewContainerRef: ViewContainerRef) { }
}

export interface ToolbarComponent {
  authenticationService: AuthenticationService;
  title: string;
}

@Component({
  selector: 'portofino-default-toolbar',
  templateUrl: './toolbar.component.html',
  styleUrls: ['./toolbar.component.css']
})
export class DefaultToolbarComponent implements ToolbarComponent {
  title: string;
  constructor(public authenticationService: AuthenticationService,public portofino: PortofinoService) {}
}

@Component({
  selector: 'portofino-app',
  templateUrl: './portofino-app.component.html',
  styleUrls: ['./portofino-app.component.css']
})
export class PortofinoAppComponent implements OnInit {

  static components: any = {};

  @Input('appTitle')
  title = 'Portofino';
  @Input()
  apiRoot: string;
  @ViewChild(ToolbarDirective)
  toolbarHost: ToolbarDirective;

  @ViewChild(NavigationDirective)
  navigationHost: NavigationDirective;
  navigation: NavigationComponent;

  constructor(public portofino: PortofinoService, public authenticationService: AuthenticationService,
              protected componentFactoryResolver: ComponentFactoryResolver, translate: TranslateService,
              @Inject(TOOLBAR_COMPONENT) protected toolbarComponent,@Inject(NAVIGATION_COMPONENT) protected navigationComponent) {
    translate.setDefaultLang('en');
    translate.setTranslation('en', TRANSLATIONS_EN, true);
    translate.setTranslation('it', TRANSLATIONS_IT, true);
    translate.use(translate.getBrowserLang());
  }

  ngOnInit(): void {
    if(this.apiRoot) {
      this.portofino.defaultApiRoot = this.apiRoot;
      this.portofino.localApiPath = null;
    }
    this.portofino.init();
    //Dynamically create the toolbar component
    let componentFactory = this.componentFactoryResolver.resolveComponentFactory(this.toolbarComponent);
    let toolbar = this.toolbarHost.viewContainerRef.createComponent(componentFactory).instance as ToolbarComponent;
    let navigationFactory = this.componentFactoryResolver.resolveComponentFactory(this.navigationComponent);
    this.navigation = this.navigationHost.viewContainerRef.createComponent(navigationFactory).instance as NavigationComponent;
    toolbar.title = this.title;
  }
}

export function PortofinoComponent(info: { name: string }) {
  return function(target) {
    PortofinoAppComponent.components[info.name] = target;
  };
}
