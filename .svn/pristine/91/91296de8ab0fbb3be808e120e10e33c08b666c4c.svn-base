.PHONY: clean

LOCAL_BUILD_DIR=/tmp/local_builds
LOCAL_BUILD_DATE=`date +'%y-%m-%d'`
LOCAL_BUILD_COMMIT_ID=local
LOCAL_BUILD_NUMBER=local
DATE := $(if $(DATE),$(DATE),$$(date +%Y-%m-%d_%H-%M-%S))
ifdef AWS_S3_ENDPOINT
	aws_ep_url = --endpoint-url $(AWS_S3_ENDPOINT)
else
	aws_ep_url =
endif

ifdef AWS_PROFILE
	aws_profile = --profile $(AWS_PROFILE)
else
	aws_profile =
endif

create_tmp_dir:
	mkdir -p $(TEMP_DIR)

zip_artifact:
	  BUILD_TIMESTAMP=$(DATE) SVN_REVISION_1=$(COMMIT_ID) BUILD_NUMBER=$(BUILD_NUMBER) sbt dist

clean:
	  sbt clean

test:
		echo "Running tests...."

upload:
		aws $(aws_ep_url) s3 cp target/universal/rms-$(DATE)_$(COMMIT_ID)_$(BUILD_NUMBER).zip s3://$(BUCKET_NAME)/rms/$(BRANCH)/rms-$(DATE)_$(COMMIT_ID)_$(BUILD_NUMBER).zip $(aws_profile)

build_zip_upload: zip_artifact upload

build_local_zip: 
	mkdir -p $(LOCAL_BUILD_DIR)
	make DATE=$(LOCAL_BUILD_DATE) COMMIT_ID=$(LOCAL_BUILD_COMMIT_ID) \
		BUILD_NUMBER=$(LOCAL_BUILD_NUMBER) zip_artifact
	mv target/universal/rms-$(LOCAL_BUILD_DATE)_$(LOCAL_BUILD_COMMIT_ID)_$(LOCAL_BUILD_NUMBER).zip \
		$(LOCAL_BUILD_DIR)/rms-$(LOCAL_BUILD_DATE)_$(LOCAL_BUILD_COMMIT_ID)_$(LOCAL_BUILD_NUMBER).zip
