package com.example.demo.configuration;

import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import com.example.demo.dto.*;
import com.example.demo.entities.*;

import java.util.HashSet;
import java.util.Set;

@Configuration
public class ModelMapperConfiguration {

    @Autowired private Environment env;

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper mapper = new ModelMapper();
        mapper.getConfiguration()
              .setMatchingStrategy(MatchingStrategies.STRICT)
              .setFieldMatchingEnabled(true)
              .setSkipNullEnabled(true);

        String imageBaseUrl    = env.getProperty("images_url", "");
        String artistBaseUrl   = env.getProperty("artist_images", "");
        String playlistBaseUrl = env.getProperty("playlist_images", "");
        String musicBaseUrl    = env.getProperty("musics_url", "");

        // === ALBUMS ===
        mapper.typeMap(Albums.class, AlbumsDTO.class)
              .addMappings(m -> {
                  m.map(Albums::getAlbumId, AlbumsDTO::setAlbumId);
                  m.map(Albums::getTitle, AlbumsDTO::setTitle);
                  m.map(Albums::getReleaseDate, AlbumsDTO::setReleaseDate);
                  m.using(ctx -> {
                      String cover = (String) ctx.getSource();
                      return cover != null && !cover.isBlank() ? imageBaseUrl + cover : null;
                  }).map(Albums::getCoverUrl, AlbumsDTO::setCoverUrl);

                  // BỎ MAP TRỰC TIẾP ARTIST & SONGS
                  m.skip(AlbumsDTO::setArtistId);
                  m.skip(AlbumsDTO::setArtistName);
                  m.skip(AlbumsDTO::setSongs);
              });

        // === ARTISTS ===
        mapper.typeMap(Artists.class, ArtistsDTO.class)
              .addMappings(m -> {
                  m.map(Artists::getArtistId, ArtistsDTO::setArtistId);
                  m.map(Artists::getName, ArtistsDTO::setName);
                  m.map(Artists::getBio, ArtistsDTO::setBio);
                  m.map(Artists::getCreatedAt, ArtistsDTO::setCreatedAt);
                  m.using(ctx -> {
                      String img = (String) ctx.getSource();
                      return img != null && !img.isBlank() ? artistBaseUrl + img : null;
                  }).map(Artists::getImageUrl, ArtistsDTO::setImageUrl);
              });

     // === SONGS ===
        mapper.typeMap(Songs.class, SongsDTO.class)
              .addMappings(m -> {
                  m.map(Songs::getSongId, SongsDTO::setSongId);
                  m.map(Songs::getTitle, SongsDTO::setTitle);
                  m.map(Songs::getDuration, SongsDTO::setDuration);
                  m.map(Songs::getLyrics, SongsDTO::setLyrics);
                  m.map(Songs::getReleaseDate, SongsDTO::setReleaseDate);
                  m.map(Songs::getStatus, SongsDTO::setStatus);
                  m.map(Songs::getListenCount, SongsDTO::setListenCount);
                  m.map(Songs::getCreatedAt, SongsDTO::setCreatedAt);

                  // File path + base URL
                  m.using(ctx -> {
                      String file = (String) ctx.getSource();
                      return file != null && !file.isBlank() ? musicBaseUrl + file : null;
                  }).map(Songs::getFilePath, SongsDTO::setFilePath);

                  // Image URL + base URL
                  m.using(ctx -> {
                      String img = (String) ctx.getSource();
                      return img != null && !img.isBlank() ? imageBaseUrl + img : null;
                  }).map(Songs::getImageUrl, SongsDTO::setImageUrl);

                  // BỎ HẾT CÁC MAP SAU (sẽ làm thủ công trong Service)
                  m.skip(SongsDTO::setAlbumId);
                  m.skip(SongsDTO::setAlbumTitle);
                  m.skip(SongsDTO::setUserId);
                  m.skip(SongsDTO::setUsername);
                  m.skip(SongsDTO::setArtists);
                  m.skip(SongsDTO::setGenres);
              });

        // === PLAYLISTS ===
        mapper.typeMap(Playlists.class, PlaylistDTO.class)
              .addMappings(m -> {
                  m.map(Playlists::getPlaylistId, PlaylistDTO::setPlaylistId);
                  m.map(Playlists::getName, PlaylistDTO::setName);
                  m.map(Playlists::getDescription, PlaylistDTO::setDescription);
                  m.map(Playlists::getCreatedAt, PlaylistDTO::setCreatedAt);
                  m.using(ctx -> {
                      String img = (String) ctx.getSource();
                      return img != null && !img.isBlank() ? playlistBaseUrl + img : null;
                  }).map(Playlists::getCoverImage, PlaylistDTO::setCoverImage);

                  m.skip(PlaylistDTO::setUserId);
                  m.skip(PlaylistDTO::setUsername);
                  m.skip(PlaylistDTO::setSongs);
              });

        return mapper;
    }
}