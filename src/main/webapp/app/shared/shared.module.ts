import { NgModule, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { JhIpsterSharedLibsModule, JhIpsterSharedCommonModule, JhiLoginModalComponent, HasAnyAuthorityDirective } from './';

@NgModule({
  imports: [JhIpsterSharedLibsModule, JhIpsterSharedCommonModule],
  declarations: [JhiLoginModalComponent, HasAnyAuthorityDirective],
  entryComponents: [JhiLoginModalComponent],
  exports: [JhIpsterSharedCommonModule, JhiLoginModalComponent, HasAnyAuthorityDirective],
  schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class JhIpsterSharedModule {
  static forRoot() {
    return {
      ngModule: JhIpsterSharedModule
    };
  }
}
