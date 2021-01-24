/*
 * Copyright (c) 2020-2040, 北京符节科技有限公司 (support@fujieid.com & https://www.fujieid.com).
 * <p>
 * Licensed under the GNU LESSER GENERAL PUBLIC LICENSE 3.0;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.gnu.org/licenses/lgpl.html
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.fujieid.jap.core;

import java.nio.charset.StandardCharsets;

/**
 * JAP constant
 *
 * @author yadong.zhang (yadong.zhang0415(a)gmail.com)
 * @version 1.0.0
 * @date 2021/1/11 18:19
 * @since 1.0.0
 */
public interface JapConst {

    String SESSION_USER_KEY = "_jap:session:user";


    /**
     * Default salt. Default salt is not recommended
     */
    byte[] DEFAULT_CREDENTIAL_ENCRYPT_SALT = "jap:123456".getBytes(StandardCharsets.UTF_8);

    /**
     * default delimiter
     */
    char DEFAULT_DELIMITER = ':';
}
