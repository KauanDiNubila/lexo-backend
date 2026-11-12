package app.lexo.adapter.out.client;

import app.lexo.common.reference.ExistsResponse;
import app.lexo.common.user.InternalUser;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "auth-service")
public interface UserServiceClient {

    @GetMapping("/internal/usuarios/{id}/existe")
    ExistsResponse userExists(@PathVariable("id") String id, @RequestParam("orgId") String orgId);

    @GetMapping("/internal/usuarios/{id}")
    InternalUser findUser(@PathVariable("id") String id, @RequestParam("orgId") String orgId);
}
