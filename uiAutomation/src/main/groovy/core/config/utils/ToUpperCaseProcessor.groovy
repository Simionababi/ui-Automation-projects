package core.config.utils

import org.aeonbits.owner.Preprocessor

class ToUpperCaseProcessor implements Preprocessor{
    @Override
    String process(String s){
        return s.toUpperCase()
    }
}
