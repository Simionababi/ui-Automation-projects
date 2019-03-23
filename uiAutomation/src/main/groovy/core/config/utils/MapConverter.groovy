package core.config.utils

import org.aeonbits.owner.Converter

import java.lang.reflect.Method

class MapConverter  implements Converter<Map>{
    @Override
    Map convert(Method method, String s){
        return (Map)Eval.me(s)
    }
}
