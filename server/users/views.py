from django.http import JsonResponse
from django.views.decorators.csrf import csrf_exempt
from django.views.decorators.http import require_http_methods

from .models import Contact, Device


@csrf_exempt
@require_http_methods(["POST"])
def register(request):
    device_id = request.POST.get("device_id")

    if not device_id:
        return JsonResponse(
            {
                "error": "Please pass a 'device_id' parameter to the server!",
                "success": False,
            }
        )

    obj, _ = Device.objects.get_or_create(device_id=device_id)

    for id_field in ("apple_id", "android_id"):
        field = request.POST.get(id_field)
        if field:
            setattr(obj, id_field, field)
            obj.save()
            break
    else:
        return JsonResponse(
            {
                "error": "Please pass either 'apple_id' or 'android_id' for push notifications!",
                "success": False,
            }
        )

    return JsonResponse({"id": obj.id})


@csrf_exempt
@require_http_methods(["POST"])
def contact(request):
    user_id = request.POST.get("id")
    device_id = request.POST.get("device_id")

    if not user_id or not device_id:
        return JsonResponse(
            {
                "error": "You need to specify both the 'user_id' and 'device_id' fields!",
                "success": False,
            }
        )

    other_user_id = request.POST.get("other_id")

    if not other_user_id:
        return JsonResponse(
            {
                "error": "You need to specify the 'other_user_id' field!",
                "success": False,
            }
        )

    try:
        self_device = Device.objects.get(device_id=device_id, id=user_id)
        other_device = Device.objects.get(id=other_user_id)
    except Device.DoesNotExist:
        return JsonResponse(
            {"error": "Own or other device does not exist!", "success": False}
        )

    Contact.objects.create(self_device=self_device, other_device=other_device)

    return JsonResponse({"success": True})


@csrf_exempt
@require_http_methods(["POST"])
def alert(request):
    pass
